package com.mv.paymentservice.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface BookingConfirmationPort {
    void confirmPaymentSucceeded(UUID bookingId, UUID paymentId, BigDecimal amount, String currency);
}
