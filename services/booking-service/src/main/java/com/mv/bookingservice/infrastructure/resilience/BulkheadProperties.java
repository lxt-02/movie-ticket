package com.mv.bookingservice.infrastructure.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * booking.resilience.bulkhead.* (one bulkhead per called service).
 *
 * @param maxConcurrentCalls calls to one service running at the same time
 * @param maxWaitMillis      how long a call waits for a free slot before failing fast (503)
 */
@ConfigurationProperties(prefix = "booking.resilience.bulkhead")
public record BulkheadProperties(Integer maxConcurrentCalls, Long maxWaitMillis) {
    public BulkheadProperties {
        if (maxConcurrentCalls == null || maxConcurrentCalls < 1) {
            maxConcurrentCalls = 20;
        }
        if (maxWaitMillis == null || maxWaitMillis < 0) {
            maxWaitMillis = 500L;
        }
    }
}
