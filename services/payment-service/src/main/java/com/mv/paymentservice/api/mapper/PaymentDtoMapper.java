package com.mv.paymentservice.api.mapper;

import com.mv.paymentservice.api.dto.response.PaymentResponse;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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
                .provider(payment.getProvider())
                .providerTransactionId(payment.getProviderTransactionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentUrl(payment.getPaymentUrl())
                .paidAt(payment.getPaidAt())
                .expiresAt(payment.getExpiresAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    public List<PaymentResponse> toResponseList(List<Payment> payments) {
        if (payments == null) {
            return List.of();
        }
        return payments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
