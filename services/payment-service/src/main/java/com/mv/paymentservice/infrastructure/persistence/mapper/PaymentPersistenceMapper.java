package com.mv.paymentservice.infrastructure.persistence.mapper;

import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.entity.PaymentTransaction;
import com.mv.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import com.mv.paymentservice.infrastructure.persistence.entity.PaymentTransactionEntity;
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
                .paymentCode(entity.getPaymentCode())
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getPaymentStatus())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .checkoutUrl(entity.getCheckoutUrl())
                .qrCode(entity.getQrCode())
                .gatewayProvider(entity.getGatewayProvider())
                .payosOrderCode(entity.getPayosOrderCode())
                .gatewayTransactionId(entity.getGatewayTransactionId())
                .idempotencyKey(entity.getIdempotencyKey())
                .failureReason(entity.getFailureReason())
                .needsRefund(entity.isNeedsRefund())
                .paidAt(entity.getPaidAt())
                .expiredAt(entity.getExpiredAt())
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
                .paymentCode(domain.getPaymentCode())
                .paymentMethod(domain.getPaymentMethod())
                .paymentStatus(domain.getStatus())
                .amount(domain.getAmount())
                .currency(domain.getCurrency())
                .checkoutUrl(domain.getCheckoutUrl())
                .qrCode(domain.getQrCode())
                .gatewayProvider(domain.getGatewayProvider())
                .payosOrderCode(domain.getPayosOrderCode())
                .gatewayTransactionId(domain.getGatewayTransactionId())
                .idempotencyKey(domain.getIdempotencyKey())
                .failureReason(domain.getFailureReason())
                .needsRefund(domain.isNeedsRefund())
                .paidAt(domain.getPaidAt())
                .expiredAt(domain.getExpiredAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public PaymentTransactionEntity toEntity(PaymentTransaction domain) {
        if (domain == null) {
            return null;
        }
        return PaymentTransactionEntity.builder()
                .id(domain.getId())
                .paymentId(domain.getPaymentId())
                .transactionCode(domain.getTransactionCode())
                .gatewayTransactionId(domain.getGatewayTransactionId())
                .transactionType(domain.getTransactionType())
                .transactionStatus(domain.getTransactionStatus())
                .amount(domain.getAmount())
                .rawResponse(domain.getRawResponse())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
