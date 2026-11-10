package com.mv.paymentservice.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Message on topic payment-commands:
 * <pre>
 * { "commandId", "aggregateId" (bookingId), "commandType": "CREATE_PAYMENT", "occurredAt",
 *   "payload": { bookingId, userId, amount, currency, provider, description, expiredAt, idempotencyKey } }
 * { "commandId", "aggregateId" (bookingId), "commandType": "CANCEL_PAYMENT", "occurredAt",
 *   "payload": { bookingId, reason } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentCommandMessage(
        UUID commandId,
        UUID aggregateId,
        String commandType,
        Instant occurredAt,
        Payload payload
) {
    public static final String CREATE_PAYMENT = "CREATE_PAYMENT";
    public static final String CANCEL_PAYMENT = "CANCEL_PAYMENT";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payload(
            UUID bookingId,
            UUID userId,
            BigDecimal amount,
            String currency,
            String provider,
            String description,
            Instant expiredAt,
            String idempotencyKey,
            String reason
    ) {
    }
}
