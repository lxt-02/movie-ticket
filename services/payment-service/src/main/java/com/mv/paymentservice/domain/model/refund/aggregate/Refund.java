package com.mv.paymentservice.domain.model.refund.aggregate;

import com.mv.paymentservice.domain.model.payment.exception.InvalidPaymentException;
import com.mv.paymentservice.domain.model.refund.enums.RefundStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Refund {

    private UUID id;
    private UUID paymentId;
    private BigDecimal amount;
    private String reason;
    private String providerRefundId;
    private RefundStatus status;
    private String idempotencyKey;
    private String requestHash;
    private Instant createdAt;
    private Instant completedAt;
    private Instant updatedAt;

    public static Refund create(UUID id, UUID paymentId, BigDecimal amount, String reason,
                                String idempotencyKey, String requestHash) {
        if (paymentId == null) {
            throw new InvalidPaymentException("Payment ID cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Refund amount must be greater than zero");
        }

        Instant now = Instant.now();
        return Refund.builder()
                .id(id != null ? id : UUID.randomUUID())
                .paymentId(paymentId)
                .amount(amount)
                .reason(reason)
                .status(RefundStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void markCompleted(String providerRefundId) {
        if (this.status == RefundStatus.COMPLETED) {
            return;
        }
        this.status = RefundStatus.COMPLETED;
        this.providerRefundId = providerRefundId;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = RefundStatus.FAILED;
        this.updatedAt = Instant.now();
    }
}
