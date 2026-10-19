package com.mv.paymentservice.application.port.in;

import java.util.UUID;

public interface CancelBookingPaymentsUseCase {
    /**
     * Handles CANCEL_PAYMENT from booking-service: closes every open payment link of the booking.
     *
     * @return number of payments cancelled
     */
    int cancelOpenPayments(UUID bookingId, String reason);
}
