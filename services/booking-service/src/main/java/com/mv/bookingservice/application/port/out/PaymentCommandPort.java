package com.mv.bookingservice.application.port.out;

import com.mv.bookingservice.domain.model.booking.aggregate.Booking;

import java.util.UUID;

/**
 * Commands for payment-service on Kafka topic payment-commands, written to the booking outbox in the caller's
 * transaction.
 */
public interface PaymentCommandPort {

    /** CREATE_PAYMENT for the booking's current payment attempt (key payment:create:PAYOS:{bookingId}:{attempt}). */
    void requestPayment(Booking booking);

    /** CANCEL_PAYMENT: close every open PayOS link of the booking (booking cancelled or expired). */
    void cancelPayments(Booking booking, String reason);
}
