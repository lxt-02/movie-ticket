package com.mv.paymentservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Built from a CREATE_PAYMENT command on Kafka topic payment-commands (sent by booking-service).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentCommand {
    private UUID commandId;
    private UUID bookingId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    /** Empty means PAYOS. Only PAYOS is supported. */
    private String provider;
    private String description;
    /** Payment deadline; booking-service sends the seat-hold expiry. Null = now + payos.payment-expiration-minutes. */
    private Instant expiredAt;
    /** Optional. Default: payment:create:PAYOS:{bookingId}. Send a new key to open a new attempt. */
    private String idempotencyKey;
}
