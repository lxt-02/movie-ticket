package com.mv.bookingservice.infrastructure.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * booking.resilience.circuit-breaker.* (same settings for the "showtime" and "user" breakers).
 *
 * @param slidingWindowSize          last N calls used to compute the failure rate
 * @param minimumNumberOfCalls       no decision before this many calls in the window
 * @param failureRateThreshold       % of failed calls that opens the breaker
 * @param waitDurationInOpenSeconds  how long OPEN rejects calls before trying HALF_OPEN
 * @param permittedCallsInHalfOpen   trial calls in HALF_OPEN; all must succeed to close again
 */
@ConfigurationProperties(prefix = "booking.resilience.circuit-breaker")
public record CircuitBreakerProperties(
        Integer slidingWindowSize,
        Integer minimumNumberOfCalls,
        Float failureRateThreshold,
        Long waitDurationInOpenSeconds,
        Integer permittedCallsInHalfOpen
) {
    public CircuitBreakerProperties {
        if (slidingWindowSize == null || slidingWindowSize <= 0) {
            slidingWindowSize = 10;
        }
        if (minimumNumberOfCalls == null || minimumNumberOfCalls <= 0) {
            minimumNumberOfCalls = 5;
        }
        if (failureRateThreshold == null || failureRateThreshold <= 0 || failureRateThreshold > 100) {
            failureRateThreshold = 50f;
        }
        if (waitDurationInOpenSeconds == null || waitDurationInOpenSeconds <= 0) {
            waitDurationInOpenSeconds = 20L;
        }
        if (permittedCallsInHalfOpen == null || permittedCallsInHalfOpen <= 0) {
            permittedCallsInHalfOpen = 3;
        }
    }
}
