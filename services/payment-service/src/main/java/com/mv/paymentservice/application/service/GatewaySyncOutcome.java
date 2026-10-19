package com.mv.paymentservice.application.service;

/**
 * Result of checking / closing a payment against PayOS.
 */
public enum GatewaySyncOutcome {
    /** PayOS reported the money: payment is now SUCCESS (or late paid). */
    PAID,
    /** Link is closed: payment is now CANCELLED / EXPIRED / FAILED. */
    CLOSED,
    /** Nothing decided yet (still PENDING/PROCESSING/UNDERPAID on PayOS, or amount mismatch). */
    STILL_OPEN
}
