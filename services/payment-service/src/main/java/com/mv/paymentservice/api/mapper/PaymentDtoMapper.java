package com.mv.paymentservice.api.mapper;

import com.mv.paymentservice.api.dto.response.PaymentResponse;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentDtoMapper {

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .paymentCode(payment.getPaymentCode())
                .provider(payment.getGatewayProvider())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .checkoutUrl(payment.getCheckoutUrl())
                .qrCode(payment.getQrCode())
                .payosOrderCode(payment.getPayosOrderCode())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .failureReason(payment.getFailureReason())
                .paidAt(payment.getPaidAt())
                .expiredAt(payment.getExpiredAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    public List<PaymentResponse> toResponseList(List<Payment> payments) {
        if (payments == null) {
            return List.of();
        }
        return payments.stream().map(this::toResponse).toList();
    }
}
