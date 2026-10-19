package com.mv.bookingservice.infrastructure.resilience;

import com.mv.bookingservice.application.exception.DependencyUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.util.function.Supplier;

/**
 * Runs a remote call through a circuit breaker.
 * <ul>
 *     <li>Breaker OPEN: {@link DependencyUnavailableException} immediately, the service is not contacted.</li>
 *     <li>Technical failure (down, timeout, 5xx): recorded by the breaker, then {@link DependencyUnavailableException}.</li>
 *     <li>Business exceptions (409 "seat taken", hold rejected...): passed through unchanged, not recorded.</li>
 * </ul>
 * Both 503 cases are temporary, so callers that retry (seat confirmation job) keep retrying.
 */
public final class ResilientCall {

    private ResilientCall() {
    }

    public static <T> T run(CircuitBreaker breaker, String serviceName, Supplier<T> call) {
        try {
            return breaker.executeSupplier(call);
        } catch (CallNotPermittedException e) {
            throw new DependencyUnavailableException(
                    serviceName + " is temporarily unavailable (circuit breaker " + breaker.getState()
                            + ": call rejected without contacting the service)", e);
        } catch (RuntimeException e) {
            if (ResilienceConfig.isTechnicalFailure(e)) {
                throw new DependencyUnavailableException(
                        serviceName + " call failed (" + e.getClass().getSimpleName() + ": " + e.getMessage()
                                + "); circuit breaker is " + breaker.getState(), e);
            }
            throw e;
        }
    }
}
