package com.mv.bookingservice.infrastructure.resilience;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Circuit breakers for the synchronous Feign calls of booking-service.
 * <p>
 * Only <b>technical</b> failures count (service down, timeout, 5xx). A business answer such as 409 "seat taken"
 * or 404 "hold not found" means the other service works fine, so it is counted as a successful call: a burst of
 * sold-out seats or refunds must never open the breaker and block normal buyers.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CircuitBreakerProperties.class)
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
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        registry.getEventPublisher().onEntryAdded(added -> logTransitions(added.getAddedEntry()));
        // Create both up front so they show in the status endpoint before the first call.
        registry.circuitBreaker(SHOWTIME);
        registry.circuitBreaker(USER);
        return registry;
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
