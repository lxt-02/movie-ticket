package com.mv.paymentservice.infrastructure.persistence.mapper;

import com.mv.paymentservice.domain.model.refund.aggregate.Refund;
import com.mv.paymentservice.infrastructure.persistence.entity.RefundEntity;
import org.springframework.stereotype.Component;

@Component
public class RefundPersistenceMapper {

    public Refund toDomain(RefundEntity entity) {
        if (entity == null) {
            return null;
        }

        return Refund.builder()
                .id(entity.getId())
                .paymentId(entity.getPaymentId())
                .amount(entity.getAmount())
                .reason(entity.getReason())
                .providerRefundId(entity.getProviderRefundId())
                .status(entity.getStatus())
                .idempotencyKey(entity.getIdempotencyKey())
                .requestHash(entity.getRequestHash())
                .createdAt(entity.getCreatedAt())
                .completedAt(entity.getCompletedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public RefundEntity toEntity(Refund domain) {
        if (domain == null) {
            return null;
        }

        return RefundEntity.builder()
                .id(domain.getId())
                .paymentId(domain.getPaymentId())
                .amount(domain.getAmount())
                .reason(domain.getReason())
                .providerRefundId(domain.getProviderRefundId())
                .status(domain.getStatus())
                .idempotencyKey(domain.getIdempotencyKey())
                .requestHash(domain.getRequestHash())
                .createdAt(domain.getCreatedAt())
                .completedAt(domain.getCompletedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
