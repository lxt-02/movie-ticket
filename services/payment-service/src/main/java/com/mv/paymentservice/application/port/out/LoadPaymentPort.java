package com.mv.paymentservice.application.port.out;

import com.mv.paymentservice.domain.model.payment.aggregate.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadPaymentPort {
    Optional<Payment> findById(UUID id);
    Optional<Payment> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
    List<Payment> findByBookingId(UUID bookingId);
}
