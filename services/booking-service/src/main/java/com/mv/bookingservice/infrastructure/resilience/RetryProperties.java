package com.mv.bookingservice.infrastructure.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * booking.resilience.retry.* (same for "showtime" and "user").
 *
 * @param maxAttempts  total attempts including the first one (1 = no retry)
 * @param waitMillis   wait before the 2nd attempt
 * @param multiplier   exponential backoff: 300ms, 600ms, ...
 */
@ConfigurationProperties(prefix = "booking.resilience.retry")
public record RetryProperties(Integer maxAttempts, Long waitMillis, Double multiplier) {
    public RetryProperties {
        if (maxAttempts == null || maxAttempts < 1) {
            maxAttempts = 3;
        }
        if (waitMillis == null || waitMillis < 1) {
            waitMillis = 300L;
        }
        if (multiplier == null || multiplier < 1) {
            multiplier = 2.0;
        }
    }
}
