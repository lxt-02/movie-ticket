package com.mv.paymentservice.application.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Envelope written to the outbox and published to payment-events:
 * { eventId, aggregateId (= bookingId), eventType, occurredAt, payload }.
 */
public record PaymentEvent(
        UUID eventId,
        UUID bookingId,
        PaymentEventType eventType,
        Instant occurredAt,
        Map<String, Object> payload
) {
}
