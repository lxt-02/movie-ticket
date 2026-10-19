package com.mv.paymentservice.application.event;

/**
 * Event types published on Kafka topic payment-events.
 */
public enum PaymentEventType {
    PAYMENT_LINK_CREATED,
    PAYMENT_CREATE_FAILED,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    PAYMENT_EXPIRED,
    /** Link closed without payment (replaced by a new attempt, booking cancelled/expired, cancelled on PayOS). */
    PAYMENT_CANCELLED
}
