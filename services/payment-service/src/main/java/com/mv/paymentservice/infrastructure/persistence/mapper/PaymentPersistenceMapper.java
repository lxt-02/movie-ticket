package com.mv.paymentservice.infrastructure.persistence.mapper;

import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentPersistenceMapper {

    public Payment toDomain(PaymentEntity entity) {
        if (entity == null) {
            return null;
        }

        return Payment.builder()
                .id(entity.getId())
                .bookingId(entity.getBookingId())
                .userId(entity.getUserId())
                .provider(entity.getProvider())
                .providerTransactionId(entity.getProviderTransactionId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .paymentUrl(entity.getPaymentUrl())
                .idempotencyKey(entity.getIdempotencyKey())
                .requestHash(entity.getRequestHash())
                .paidAt(entity.getPaidAt())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PaymentEntity toEntity(Payment domain) {
        if (domain == null) {
            return null;
        }

        return PaymentEntity.builder()
                .id(domain.getId())
                .bookingId(domain.getBookingId())
                .userId(domain.getUserId())
                .provider(domain.getProvider())
                .providerTransactionId(domain.getProviderTransactionId())
                .amount(domain.getAmount())
                .currency(domain.getCurrency())
                .status(domain.getStatus())
                .paymentUrl(domain.getPaymentUrl())
                .idempotencyKey(domain.getIdempotencyKey())
                .requestHash(domain.getRequestHash())
                .paidAt(domain.getPaidAt())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
