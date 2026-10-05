package com.mv.paymentservice.infrastructure.persistence.repository;

import com.mv.paymentservice.infrastructure.persistence.entity.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundJpaRepository extends JpaRepository<RefundEntity, UUID> {
    Optional<RefundEntity> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey);
    List<RefundEntity> findByPaymentId(UUID paymentId);
}
