package com.mv.bookingservice.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * booking.kafka.* settings.
 *
 * @param paymentCommands         topic for CREATE_PAYMENT / CANCEL_PAYMENT (consumed by payment-service)
 * @param paymentEvents           topic with PAYMENT_* events (produced by payment-service); failed events go to
 *                                {@code <paymentEvents>.DLT}
 * @param relayBatchSize          outbox rows published per relay run
 * @param publishTimeoutMs        wait for the broker ack
 * @param eventRetryMaxIntervalMs longest pause between two attempts of a failing payment event
 * @param eventRetryMaxElapsedMs  a payment event that still fails after this long goes to the dead-letter topic
 */
@ConfigurationProperties(prefix = "booking.kafka")
public record BookingKafkaProperties(
        String paymentCommands,
        String paymentEvents,
        Integer relayBatchSize,
        Long publishTimeoutMs,
        Long eventRetryMaxIntervalMs,
        Long eventRetryMaxElapsedMs
) {
    public BookingKafkaProperties {
        if (paymentCommands == null || paymentCommands.isBlank()) {
            paymentCommands = "payment-commands";
        }
        if (paymentEvents == null || paymentEvents.isBlank()) {
            paymentEvents = "payment-events";
        }
        if (relayBatchSize == null || relayBatchSize <= 0) {
            relayBatchSize = 50;
        }
        if (publishTimeoutMs == null || publishTimeoutMs <= 0) {
            publishTimeoutMs = 5000L;
        }
        if (eventRetryMaxIntervalMs == null || eventRetryMaxIntervalMs <= 0) {
            eventRetryMaxIntervalMs = 10_000L;
        }
        if (eventRetryMaxElapsedMs == null || eventRetryMaxElapsedMs <= 0) {
            eventRetryMaxElapsedMs = 60_000L;
        }
    }

    /** Dead-letter topic of payment-events. */
    public String paymentEventsDlt() {
        return paymentEvents + ".DLT";
    }
}
