package com.mv.bookingservice.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * booking.kafka.* settings.
 *
 * @param paymentCommands           topic for CREATE_PAYMENT / CANCEL_PAYMENT (consumed by payment-service)
 * @param paymentEvents             topic with PAYMENT_* events (produced by payment-service); failed events go to
 *                                  {@code <paymentEvents>.DLT}
 * @param relayBatchSize            outbox rows published per relay run
 * @param publishTimeoutMs          wait for the broker ack. Keep it above the producer's max.block.ms +
 *                                  delivery.timeout.ms, so the producer never sends after the relay gave up
 * @param eventRetryMaxIntervalMs   longest pause between two attempts of a failing payment event
 * @param eventRetryMaxElapsedMs    a payment event that still fails after this long goes to the dead-letter topic
 * @param relayRetryInitialDelayMs  first back-off after a temporary publish error (then doubled)
 * @param relayRetryMaxDelayMs      longest back-off of the outbox relay
 * @param relayAlertAfterAttempts   temporary errors of one row logged as ERROR from this attempt on
 * @param verifyTopics              check on startup that the topics booking needs exist (wrong name, missing topic)
 */
@ConfigurationProperties(prefix = "booking.kafka")
// [VI] Cấu hình booking.kafka.* trong application.yaml. Tên topic mặc định (payment-commands, payment-events)
// phải khớp contracts/booking-payment/contract.json -> topics. Event lỗi hẳn đi sang payment-events.DLT.
public record BookingKafkaProperties(
        String paymentCommands,
        String paymentEvents,
        Integer relayBatchSize,
        Long publishTimeoutMs,
        Long eventRetryMaxIntervalMs,
        Long eventRetryMaxElapsedMs,
        Long relayRetryInitialDelayMs,
        Long relayRetryMaxDelayMs,
        Integer relayAlertAfterAttempts,
        Boolean verifyTopics
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
            publishTimeoutMs = 15_000L;
        }
        if (eventRetryMaxIntervalMs == null || eventRetryMaxIntervalMs <= 0) {
            eventRetryMaxIntervalMs = 10_000L;
        }
        if (eventRetryMaxElapsedMs == null || eventRetryMaxElapsedMs <= 0) {
            eventRetryMaxElapsedMs = 60_000L;
        }
        if (relayRetryInitialDelayMs == null || relayRetryInitialDelayMs <= 0) {
            relayRetryInitialDelayMs = 2_000L;
        }
        if (relayRetryMaxDelayMs == null || relayRetryMaxDelayMs <= 0) {
            relayRetryMaxDelayMs = 60_000L;
        }
        if (relayAlertAfterAttempts == null || relayAlertAfterAttempts <= 0) {
            relayAlertAfterAttempts = 10;
        }
        if (verifyTopics == null) {
            verifyTopics = true;
        }
    }

    /** Dead-letter topic of payment-events. */
    public String paymentEventsDlt() {
        return paymentEvents + ".DLT";
    }
}
