package com.mv.paymentservice.domain.model.payment.aggregate;

import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import com.mv.paymentservice.domain.model.payment.exception.InvalidPaymentException;
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
public class Payment {

    private UUID id;
    private UUID bookingId;
    private UUID userId;
    private String provider;
    private String providerTransactionId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String paymentUrl;
    private String idempotencyKey;
    private String requestHash;
    private Instant paidAt;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static Payment create(UUID id, UUID bookingId, UUID userId, String provider,
                                 BigDecimal amount, String currency, String paymentUrl,
                                 String idempotencyKey, String requestHash, Instant expiresAt) {
        if (bookingId == null) {
            throw new InvalidPaymentException("Booking ID cannot be null");
        }
        if (userId == null) {
            throw new InvalidPaymentException("User ID cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Amount must be greater than zero");
        }
        if (provider == null || provider.isBlank()) {
            throw new InvalidPaymentException("Provider cannot be blank");
        }

        Instant now = Instant.now();
        return Payment.builder()
                .id(id != null ? id : UUID.randomUUID())
                .bookingId(bookingId)
                .userId(userId)
                .provider(provider)
                .amount(amount)
                .currency(currency != null && !currency.isBlank() ? currency : "VND")
                .status(PaymentStatus.PENDING)
                .paymentUrl(paymentUrl)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void markSuccess(String providerTransactionId, Instant paidAt) {
        if (this.status == PaymentStatus.SUCCESS) {
            return; // Already successful
        }
        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidPaymentException("Cannot transition payment " + id + " from " + status + " to SUCCESS");
        }
        this.status = PaymentStatus.SUCCESS;
        this.providerTransactionId = providerTransactionId;
        this.paidAt = paidAt != null ? paidAt : Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        if (this.status == PaymentStatus.FAILED) {
            return;
        }
        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidPaymentException("Cannot transition payment " + id + " from " + status + " to FAILED");
        }
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public void markRefundPending() {
        if (this.status != PaymentStatus.SUCCESS) {
            throw new InvalidPaymentException("Only SUCCESS payments can be refunded");
        }
        this.status = PaymentStatus.REFUND_PENDING;
        this.updatedAt = Instant.now();
    }

    public void markRefunded() {
        if (this.status != PaymentStatus.REFUND_PENDING && this.status != PaymentStatus.SUCCESS) {
            throw new InvalidPaymentException("Cannot transition payment " + id + " from " + status + " to REFUNDED");
        }
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }
}
