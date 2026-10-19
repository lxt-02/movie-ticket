package com.mv.paymentservice.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * payment.kafka.* settings.
 *
 * @param paymentCommands topic consumed by this service (CREATE_PAYMENT from booking-service)
 * @param paymentEvents   topic produced by this service (PAYMENT_* events for booking-service)
 * @param partitions      partitions used when the topics are auto-created
 */
@ConfigurationProperties(prefix = "payment.kafka")
public record KafkaTopicsProperties(String paymentCommands, String paymentEvents, Integer partitions) {
    public KafkaTopicsProperties {
        if (paymentCommands == null || paymentCommands.isBlank()) {
            paymentCommands = "payment-commands";
        }
        if (paymentEvents == null || paymentEvents.isBlank()) {
            paymentEvents = "payment-events";
        }
        if (partitions == null || partitions <= 0) {
            partitions = 3;
        }
    }
}
