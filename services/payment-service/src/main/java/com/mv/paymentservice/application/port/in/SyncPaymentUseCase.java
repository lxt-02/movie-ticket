package com.mv.paymentservice.application.port.in;

import com.mv.paymentservice.domain.model.payment.aggregate.Payment;

import java.util.UUID;

/**
 * Asks PayOS for the real state of a payment right now (frontend calls it when the buyer lands on returnUrl).
 * The redirect parameters themselves are never trusted.
 */
public interface SyncPaymentUseCase {
    Payment syncPayment(UUID paymentId);

    Payment syncLatestPaymentOfBooking(UUID bookingId);
}
