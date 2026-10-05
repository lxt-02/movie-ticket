package com.mv.paymentservice.domain.repository;

import com.mv.paymentservice.domain.model.refund.aggregate.Refund;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository {
    Refund save(Refund refund);
    Optional<Refund> findById(UUID id);
    Optional<Refund> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey);
    List<Refund> findByPaymentId(UUID paymentId);
}
