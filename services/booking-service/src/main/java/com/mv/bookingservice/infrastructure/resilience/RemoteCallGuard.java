package com.mv.bookingservice.infrastructure.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

import java.util.function.Supplier;

/**
 * The resilience of ONE called service: Retry → CircuitBreaker → Bulkhead → Feign call.
 * {@code retry} / {@code bulkhead} may be null (not used, e.g. in unit tests of the breaker alone).
 */
public record RemoteCallGuard(String serviceName, CircuitBreaker breaker, Retry retry, Bulkhead bulkhead) {

    public static RemoteCallGuard breakerOnly(String serviceName, CircuitBreaker breaker) {
        return new RemoteCallGuard(serviceName, breaker, null, null);
    }

    public <T> T call(Supplier<T> remoteCall) {
        return ResilientCall.run(this, remoteCall);
    }
}
