package com.mv.paymentservice.infrastructure.persistence.repository;

import com.mv.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {
    Optional<PaymentEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
    List<PaymentEntity> findByBookingId(UUID bookingId);
}
