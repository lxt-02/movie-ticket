package com.mv.bookingservice.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * booking.kafka.* settings.
 *
 * @param paymentCommands  topic for CREATE_PAYMENT (consumed by payment-service)
 * @param paymentEvents    topic with PAYMENT_* events (produced by payment-service)
 * @param relayBatchSize   outbox rows published per relay run
 * @param publishTimeoutMs wait for the broker ack
 */
@ConfigurationProperties(prefix = "booking.kafka")
public record BookingKafkaProperties(
        String paymentCommands,
        String paymentEvents,
        Integer relayBatchSize,
        Long publishTimeoutMs
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
    }
}
