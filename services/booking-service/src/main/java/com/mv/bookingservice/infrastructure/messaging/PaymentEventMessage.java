package com.mv.bookingservice.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Envelope on topic payment-events: { eventId, aggregateId (bookingId), eventType, occurredAt, payload }.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentEventMessage(
        UUID eventId,
        UUID aggregateId,
        String eventType,
        Instant occurredAt,
        Payload payload
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payload(
            UUID paymentId,
            UUID bookingId,
            String paymentStatus,
            BigDecimal amount,
            String currency,
            String reason,
            String message,
            String failureReason,
            Instant paidAt
    ) {
    }
}
