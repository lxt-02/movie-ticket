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
    private String paymentCode;
    private String provider;
    private String paymentMethod;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String checkoutUrl;
    private String qrCode;
    private Long payosOrderCode;
    private String gatewayTransactionId;
    private String failureReason;
    private Instant paidAt;
    private Instant expiredAt;
    private Instant createdAt;
    private Instant updatedAt;
}
