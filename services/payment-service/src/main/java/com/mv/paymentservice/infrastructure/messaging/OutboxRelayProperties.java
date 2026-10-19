package com.mv.paymentservice.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * payment.outbox.* settings for the outbox relay.
 */
@ConfigurationProperties(prefix = "payment.outbox")
public record OutboxRelayProperties(
        Integer batchSize,
        Integer maxAttempts,
        Long publishTimeoutMs,
        Long retryDelayMs,
        Long stalePublishingTimeoutMs
) {
    public OutboxRelayProperties {
        if (batchSize == null || batchSize <= 0) {
            batchSize = 50;
        }
        if (maxAttempts == null || maxAttempts <= 0) {
            maxAttempts = 10;
        }
        if (publishTimeoutMs == null || publishTimeoutMs <= 0) {
            publishTimeoutMs = 5000L;
        }
        if (retryDelayMs == null || retryDelayMs <= 0) {
            retryDelayMs = 5000L;
        }
        if (stalePublishingTimeoutMs == null || stalePublishingTimeoutMs <= 0) {
            stalePublishingTimeoutMs = 120000L;
        }
    }
}
