package com.mv.bookingservice.infrastructure.resilience;

import com.mv.bookingservice.application.exception.DependencyUnavailableException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Runs a remote call through the resilience layers, from outside to inside:
 * <pre>
 *   Retry ( CircuitBreaker ( Bulkhead ( Feign call ) ) )
 * </pre>
 * <ul>
 *     <li><b>Bulkhead</b>: at most N calls to this service at the same time. Full (service slow, every slot busy):
 *     {@link DependencyUnavailableException} "đang quá tải" without calling. Not counted by the breaker.</li>
 *     <li><b>CircuitBreaker</b>: OPEN → {@link DependencyUnavailableException} immediately, the service is not
 *     contacted; technical failures (down, timeout, 5xx) are recorded.</li>
 *     <li><b>Retry</b>: a short technical failure (connection refused, timeout, 502/503/504) is tried again with
 *     backoff; every attempt goes through the breaker, so a dead service still opens it. Never retried: business
 *     answers (4xx), breaker OPEN, bulkhead full.</li>
 *     <li>Business exceptions (409 "seat taken", hold rejected...): passed through unchanged.</li>
 * </ul>
 * All 503 cases are temporary, so callers that retry later (seat confirmation job) keep retrying.
 * Every call of booking-service to showtime / user is idempotent (holds are keyed by bookingId), so retrying is safe.
 */
@Slf4j
public final class ResilientCall {

    private ResilientCall() {
    }

    /** Message for the user (HTTP 503); technical details go to the log. */
    static String unavailable(String serviceName) {
        return "Dịch vụ " + label(serviceName) + " tạm thời không khả dụng, vui lòng thử lại sau";
    }

    static String busy(String serviceName) {
        return "Dịch vụ " + label(serviceName) + " đang quá tải, vui lòng thử lại sau ít giây";
    }

    private static String label(String serviceName) {
        return switch (serviceName) {
            case "showtime-service" -> "suất chiếu";
            case "user-service" -> "người dùng";
            default -> serviceName;
        };
    }

    /** Breaker only (kept for callers / tests that do not need retry and bulkhead). */
    public static <T> T run(CircuitBreaker breaker, String serviceName, Supplier<T> call) {
        return run(RemoteCallGuard.breakerOnly(serviceName, breaker), call);
    }

    public static <T> T run(RemoteCallGuard guard, Supplier<T> call) {
        Supplier<T> decorated = call;
        if (guard.bulkhead() != null) {
            decorated = Bulkhead.decorateSupplier(guard.bulkhead(), decorated);
        }
        decorated = CircuitBreaker.decorateSupplier(guard.breaker(), decorated);
        if (guard.retry() != null) {
            decorated = Retry.decorateSupplier(guard.retry(), decorated);
        }
        String serviceName = guard.serviceName();
        try {
            return decorated.get();
        } catch (CallNotPermittedException e) {
            log.warn("{} rejected by circuit breaker {} without calling the service",
                    serviceName, guard.breaker().getState());
            throw new DependencyUnavailableException(unavailable(serviceName), e);
        } catch (BulkheadFullException e) {
            log.warn("{} bulkhead full: {} calls already running, call rejected", serviceName,
                    guard.bulkhead() != null ? guard.bulkhead().getBulkheadConfig().getMaxConcurrentCalls() : "?");
            throw new DependencyUnavailableException(busy(serviceName), e);
        } catch (RuntimeException e) {
            if (ResilienceConfig.isTechnicalFailure(e)) {
                log.warn("{} call failed ({}: {}); circuit breaker is {}", serviceName,
                        e.getClass().getSimpleName(), e.getMessage(), guard.breaker().getState());
                throw new DependencyUnavailableException(unavailable(serviceName), e);
            }
            throw e;
        }
    }
}
