package com.mv.paymentservice.api.dto.response;

import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID bookingId;
    private UUID userId;
    private String provider;
    private String providerTransactionId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String paymentUrl;
    private Instant paidAt;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}
