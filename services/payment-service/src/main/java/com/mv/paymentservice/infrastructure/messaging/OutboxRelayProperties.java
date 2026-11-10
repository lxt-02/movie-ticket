package com.mv.paymentservice.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * payment.outbox.* settings for the outbox relay.
 *
 * @param batchSize                messages claimed per run
 * @param maxAttempts              temporary errors of one message are logged as ERROR (alert) from this attempt on.
 *                                 A temporary error never makes a message FAILED: a long broker outage must not
 *                                 lose PAYMENT_SUCCEEDED. Only permanent errors (no permission, record too large)
 *                                 do.
 * @param publishTimeoutMs         wait for the broker ack; keep it above the producer's max.block.ms +
 *                                 delivery.timeout.ms
 * @param retryDelayMs             first back-off after a temporary error (then doubled)
 * @param stalePublishingTimeoutMs PROCESSING older than this (instance died) is claimed again
 * @param maxRetryDelayMs          longest back-off
 */
@ConfigurationProperties(prefix = "payment.outbox")
public record OutboxRelayProperties(
        Integer batchSize,
        Integer maxAttempts,
        Long publishTimeoutMs,
        Long retryDelayMs,
        Long stalePublishingTimeoutMs,
        Long maxRetryDelayMs
) {
    public OutboxRelayProperties {
        if (batchSize == null || batchSize <= 0) {
            batchSize = 50;
        }
        if (maxAttempts == null || maxAttempts <= 0) {
            maxAttempts = 10;
        }
        if (publishTimeoutMs == null || publishTimeoutMs <= 0) {
            publishTimeoutMs = 15_000L;
        }
        if (retryDelayMs == null || retryDelayMs <= 0) {
            retryDelayMs = 5000L;
        }
        if (stalePublishingTimeoutMs == null || stalePublishingTimeoutMs <= 0) {
            stalePublishingTimeoutMs = 120000L;
        }
        if (maxRetryDelayMs == null || maxRetryDelayMs <= 0) {
            maxRetryDelayMs = 300_000L;
        }
    }
}
