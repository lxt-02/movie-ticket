package com.mv.paymentservice.domain.model.payment.enums;

/**
 * Outcome of applying a successful PayOS webhook to a payment.
 */
public enum PaidTransition {
    /** PENDING/PROCESSING -> SUCCESS. Publish PAYMENT_SUCCEEDED. */
    MARKED_SUCCESS,
    /** Already SUCCESS (duplicate webhook). Nothing to do. */
    ALREADY_SUCCESS,
    /** Money arrived after the payment became EXPIRED/FAILED/CANCELLED. Status kept, no event, only logged. */
    LATE_PAID
}
