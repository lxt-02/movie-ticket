package com.mv.paymentservice.infrastructure.persistence.repository;

import com.mv.paymentservice.infrastructure.persistence.entity.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentTransactionJpaRepository extends JpaRepository<PaymentTransactionEntity, UUID> {

    boolean existsByPaymentIdAndTransactionCodeAndGatewayTransactionId(
            UUID paymentId, String transactionCode, String gatewayTransactionId);
}
