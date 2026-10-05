package com.mv.paymentservice.domain.repository;

import com.mv.paymentservice.domain.model.payment.aggregate.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    Optional<Payment> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
    List<Payment> findByBookingId(UUID bookingId);
}
