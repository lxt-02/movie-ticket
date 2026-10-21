package com.mv.paymentservice.domain.model.payment.aggregate;

import com.mv.paymentservice.domain.model.payment.enums.PaidTransition;
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

/**
 * Payment aggregate for one PayOS payment link of a booking.
 * <p>
 * Business rules:
 * <ul>
 *     <li>VND only, amount is a positive whole number.</li>
 *     <li>SUCCESS is never overwritten.</li>
 *     <li>A success webhook after another terminal status is a "late paid": status kept, only logged.</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Payment {

    public static final String PROVIDER_PAYOS = "PAYOS";
    public static final String METHOD_PAYOS = "PAYOS";
    public static final String CURRENCY_VND = "VND";

    private static final long ORDER_CODE_BASE = 1_000_000_000_000L;
    private static final long ORDER_CODE_RANGE = 8_000_000_000_000L;

    private UUID id;
    private UUID bookingId;
    private UUID userId;
    private String paymentCode;
    private String paymentMethod;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String checkoutUrl;
    private String qrCode;
    private String gatewayProvider;
    private Long payosOrderCode;
    private String gatewayTransactionId;
    private String idempotencyKey;
    private String failureReason;
    /** Money arrived on a link already closed (late paid): to be refunded by hand, no refund flow here. */
    private boolean needsRefund;
    private Instant paidAt;
    private Instant expiredAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static Payment createPending(UUID bookingId, UUID userId, BigDecimal amount, String currency,
                                        String idempotencyKey, Instant expiredAt, Instant now) {
        if (bookingId == null) {
            throw new InvalidPaymentException("Mã booking là bắt buộc");
        }
        if (userId == null) {
            throw new InvalidPaymentException("Người dùng là bắt buộc");
        }
        validateVndAmount(amount);
        String resolvedCurrency = currency == null || currency.isBlank() ? CURRENCY_VND : currency.trim().toUpperCase();
        if (!CURRENCY_VND.equals(resolvedCurrency)) {
            throw new InvalidPaymentException("PayOS chỉ hỗ trợ VND, nhận được: " + currency);
        }
        if (expiredAt == null || !expiredAt.isAfter(now)) {
            throw new InvalidPaymentException("Thời hạn thanh toán phải ở tương lai");
        }

        UUID id = UUID.randomUUID();
        long orderCode = generateOrderCode(id);
        return Payment.builder()
                .id(id)
                .bookingId(bookingId)
                .userId(userId)
                .paymentCode("PAY-" + orderCode)
                .paymentMethod(METHOD_PAYOS)
                .status(PaymentStatus.PENDING)
                .amount(amount.setScale(2))
                .currency(CURRENCY_VND)
                .gatewayProvider(PROVIDER_PAYOS)
                .payosOrderCode(orderCode)
                .idempotencyKey(idempotencyKey)
                .expiredAt(expiredAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * PayOS orderCode: positive BIGINT (max 9007199254740991), unique. Derived from the payment UUID,
     * range [1_000_000_000_000, 9_000_000_000_000). The unique index on payos_order_code catches collisions.
     */
    public static long generateOrderCode(UUID paymentId) {
        long mixed = paymentId.getMostSignificantBits() ^ Long.rotateLeft(paymentId.getLeastSignificantBits(), 32);
        return ORDER_CODE_BASE + Math.floorMod(mixed, ORDER_CODE_RANGE);
    }

    public static void validateVndAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidPaymentException("Số tiền phải lớn hơn 0");
        }
        if (amount.stripTrailingZeros().scale() > 0) {
            throw new InvalidPaymentException("Số tiền VND phải là số nguyên, nhận được: " + amount.toPlainString());
        }
    }

    public long amountAsLong() {
        return amount.longValueExact();
    }

    public void attachPaymentLink(String paymentLinkId, String checkoutUrl, String qrCode, Instant now) {
        this.gatewayTransactionId = paymentLinkId;
        this.checkoutUrl = checkoutUrl;
        this.qrCode = qrCode;
        this.updatedAt = now;
    }

    /** PENDING link that the buyer can still open. */
    public boolean isReusable(Instant now) {
        return status == PaymentStatus.PENDING
                && checkoutUrl != null
                && expiredAt != null
                && expiredAt.isAfter(now);
    }

    /**
     * @param paidAt time of the bank transaction reported by PayOS (falls back to now when unknown)
     * @param now    processing time
     */
    public PaidTransition markPaid(String paymentLinkId, Instant paidAt, Instant now) {
        Instant effectivePaidAt = paidAt != null ? paidAt : now;
        if (status == PaymentStatus.SUCCESS) {
            return PaidTransition.ALREADY_SUCCESS;
        }
        if (status.isTerminal()) {
            if (this.gatewayTransactionId == null) {
                this.gatewayTransactionId = paymentLinkId;
            }
            this.paidAt = effectivePaidAt;
            this.failureReason = "Late PayOS success webhook after terminal payment status: " + status;
            this.needsRefund = true;
            this.updatedAt = now;
            return PaidTransition.LATE_PAID;
        }
        this.status = PaymentStatus.SUCCESS;
        if (this.gatewayTransactionId == null) {
            this.gatewayTransactionId = paymentLinkId;
        }
        this.paidAt = effectivePaidAt;
        this.failureReason = null;
        this.updatedAt = now;
        return PaidTransition.MARKED_SUCCESS;
    }

    /** @return true when the status actually changed. */
    public boolean markFailed(String reason, Instant now) {
        return moveToTerminal(PaymentStatus.FAILED, reason, now);
    }

    /** @return true when the status actually changed. */
    public boolean markExpired(String reason, Instant now) {
        return moveToTerminal(PaymentStatus.EXPIRED, reason, now);
    }

    /** Link closed before payment (replaced by a new attempt, booking cancelled, cancelled on PayOS). */
    public boolean markCancelled(String reason, Instant now) {
        return moveToTerminal(PaymentStatus.CANCELLED, reason, now);
    }

    /** Moves to FAILED, EXPIRED or CANCELLED. @return true when the status actually changed. */
    public boolean close(PaymentStatus target, String reason, Instant now) {
        if (target != PaymentStatus.FAILED && target != PaymentStatus.EXPIRED && target != PaymentStatus.CANCELLED) {
            throw new IllegalArgumentException("Not a closing status: " + target);
        }
        return moveToTerminal(target, reason, now);
    }

    private boolean moveToTerminal(PaymentStatus target, String reason, Instant now) {
        if (status.isTerminal()) {
            // SUCCESS is never overwritten; other terminal states stay as they are.
            return false;
        }
        this.status = target;
        this.failureReason = reason;
        this.updatedAt = now;
        return true;
    }
}
