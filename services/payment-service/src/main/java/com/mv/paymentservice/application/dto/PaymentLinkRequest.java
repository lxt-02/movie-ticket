package com.mv.paymentservice.application.dto;

import java.time.Instant;

/**
 * Data needed to create a PayOS payment link.
 *
 * @param orderCode   unique BIGINT order code sent to PayOS
 * @param amount      whole VND amount
 * @param description short transfer description (PayOS limits it to 25 characters)
 * @param expiredAt   link deadline, sent to PayOS as epoch seconds
 */
public record PaymentLinkRequest(long orderCode, long amount, String description, Instant expiredAt) {
}
