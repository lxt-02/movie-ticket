package com.mv.paymentservice.application.port.in;

import java.util.UUID;

public interface RequestRefundUseCase {
    /**
     * Handles REFUND_PAYMENT from booking-service: the booking could not use a payment that was made
     * (paid too late, booking already cancelled, seats lost, amount mismatch, duplicate payment).
     *
     * @return true if a new refund was opened
     */
    boolean requestRefund(UUID bookingId, UUID paymentId, String reason);
}
