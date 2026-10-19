package com.mv.paymentservice.application.dto;

import java.time.Duration;

/**
 * Tunables used by the use cases (built from payos.* / payment.* properties).
 *
 * @param paymentExpiration   link lifetime when the command has no expiredAt
 * @param expirationBatchSize max payments expired per scheduler run
 * @param reconcileMinAge     a PENDING payment is checked against PayOS only after this age (webhook gets a head start)
 * @param reconcileBatchSize  max payments checked per reconcile run
 */
public record PaymentDefaults(
        Duration paymentExpiration,
        int expirationBatchSize,
        Duration reconcileMinAge,
        int reconcileBatchSize
) {
}
