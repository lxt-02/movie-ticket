package com.mv.bookingservice.infrastructure.resilience;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Circuit breakers for the synchronous Feign calls of booking-service.
 * <p>
 * Only <b>technical</b> failures count (service down, timeout, 5xx). A business answer such as 409 "seat taken"
 * or 404 "hold not found" means the other service works fine, so it is counted as a successful call: a burst of
 * sold-out seats must never open the breaker and block normal buyers.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({CircuitBreakerProperties.class, RetryProperties.class, BulkheadProperties.class})
public class ResilienceConfig {

    public static final String SHOWTIME = "showtime";
    public static final String USER = "user";

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerProperties props) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(props.slidingWindowSize())
                .minimumNumberOfCalls(props.minimumNumberOfCalls())
                .failureRateThreshold(props.failureRateThreshold())
                .waitDurationInOpenState(Duration.ofSeconds(props.waitDurationInOpenSeconds()))
                .permittedNumberOfCallsInHalfOpenState(props.permittedCallsInHalfOpen())
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordException(ResilienceConfig::isTechnicalFailure)
                // "Bulkhead full" says nothing about the health of the other service: neither failure nor success.
                .ignoreExceptions(BulkheadFullException.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        registry.getEventPublisher().onEntryAdded(added -> logTransitions(added.getAddedEntry()));
        // Create both up front so they show in the status endpoint before the first call.
        registry.circuitBreaker(SHOWTIME);
        registry.circuitBreaker(USER);
        return registry;
    }

    @Bean
    public RetryRegistry retryRegistry(RetryProperties props) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(props.maxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        Duration.ofMillis(props.waitMillis()), props.multiplier()))
                .retryOnException(ResilienceConfig::isRetryable)
                .build();
        RetryRegistry registry = RetryRegistry.of(config);
        registry.getEventPublisher().onEntryAdded(added -> added.getAddedEntry().getEventPublisher()
                .onRetry(event -> log.warn("RETRY [{}] attempt {} after {}: {}", event.getName(),
                        event.getNumberOfRetryAttempts(), event.getWaitInterval(),
                        event.getLastThrowable() == null ? "" : event.getLastThrowable().toString())));
        registry.retry(SHOWTIME);
        registry.retry(USER);
        return registry;
    }

    @Bean
    public BulkheadRegistry bulkheadRegistry(BulkheadProperties props) {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(props.maxConcurrentCalls())
                .maxWaitDuration(Duration.ofMillis(props.maxWaitMillis()))
                .build();
        BulkheadRegistry registry = BulkheadRegistry.of(config);
        registry.bulkhead(SHOWTIME);
        registry.bulkhead(USER);
        return registry;
    }

    /**
     * Retry only failures that are quick and likely to pass on the next try:
     * <ul>
     *     <li>connection refused / reset / unknown host (instance restarting, Eureka not updated yet);</li>
     *     <li>502 / 503 / 504 (no instance yet, upstream timeout).</li>
     * </ul>
     * NOT retried: read timeout (the service is slow: retrying multiplies the load and the wait), 4xx business
     * answers, breaker OPEN, bulkhead full.
     */
    public static boolean isRetryable(Throwable t) {
        if (t instanceof FeignException fe) {
            int status = fe.status();
            if (status == 502 || status == 503 || status == 504) {
                return true;
            }
            return status < 0 && !hasCause(fe, SocketTimeoutException.class) && !hasCause(fe, TimeoutException.class);
        }
        return t instanceof ConnectException;
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (type.isInstance(c)) {
                return true;
            }
        }
        return false;
    }

    /** True for failures that say "the other service is unhealthy"; false for business answers (4xx). */
    public static boolean isTechnicalFailure(Throwable t) {
        if (t instanceof FeignException fe) {
            int status = fe.status();
            // -1: connection refused / reset, read timeout (RetryableException); 5xx: server error;
            // 503 also when the load balancer has no instance of the service in Eureka.
            return status < 0 || status >= 500;
        }
        return t instanceof IOException
                || t instanceof TimeoutException
                || t instanceof IllegalStateException; // 2xx with an unusable body (see adapters' requireData)
    }

    private static void logTransitions(CircuitBreaker breaker) {
        breaker.getEventPublisher().onStateTransition(event ->
                log.warn("CIRCUIT BREAKER [{}] {}", event.getCircuitBreakerName(), event.getStateTransition()));
        breaker.getEventPublisher().onCallNotPermitted(event ->
                log.warn("CIRCUIT BREAKER [{}] OPEN: call rejected without contacting the service",
                        event.getCircuitBreakerName()));
    }
}
