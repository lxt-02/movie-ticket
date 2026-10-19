package com.mv.paymentservice.domain;

import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.enums.PaidTransition;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import com.mv.paymentservice.domain.model.payment.exception.InvalidPaymentException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static final Instant NOW = Instant.parse("2026-10-18T13:00:00Z");

    private Payment newPayment() {
        return Payment.createPending(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("150000.00"), "VND",
                "key", NOW.plusSeconds(900), NOW);
    }

    @Test
    void orderCodeIsPositiveAndWithinPayosLimit() {
        for (int i = 0; i < 1000; i++) {
            long code = Payment.generateOrderCode(UUID.randomUUID());
            assertThat(code).isBetween(1_000_000_000_000L, 9_000_000_000_000L - 1);
        }
    }

    @Test
    void createPendingRejectsFractionalOrForeignCurrency() {
        UUID booking = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        assertThatThrownBy(() -> Payment.createPending(booking, user, new BigDecimal("1000.50"), "VND",
                "k", NOW.plusSeconds(60), NOW)).isInstanceOf(InvalidPaymentException.class);
        assertThatThrownBy(() -> Payment.createPending(booking, user, new BigDecimal("1000"), "USD",
                "k", NOW.plusSeconds(60), NOW)).isInstanceOf(InvalidPaymentException.class);
        assertThatThrownBy(() -> Payment.createPending(booking, user, BigDecimal.ZERO, "VND",
                "k", NOW.plusSeconds(60), NOW)).isInstanceOf(InvalidPaymentException.class);
    }

    @Test
    void successIsNeverOverwritten() {
        Payment payment = newPayment();
        assertThat(payment.markPaid("link-1", NOW, NOW)).isEqualTo(PaidTransition.MARKED_SUCCESS);

        assertThat(payment.markFailed("late failure", NOW)).isFalse();
        assertThat(payment.markExpired("deadline", NOW)).isFalse();
        assertThat(payment.markPaid("link-1", NOW, NOW)).isEqualTo(PaidTransition.ALREADY_SUCCESS);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void cancelledPaymentCannotBecomeSuccessOrBeReopened() {
        Payment payment = newPayment();
        assertThat(payment.markCancelled("Replaced by a new payment attempt", NOW)).isTrue();
        assertThat(payment.markExpired("deadline", NOW)).isFalse();
        assertThat(payment.markPaid("link-1", NOW, NOW)).isEqualTo(PaidTransition.LATE_PAID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void successAfterExpiryIsLatePaidAndKeepsStatus() {
        Payment payment = newPayment();
        payment.markExpired("deadline", NOW);

        PaidTransition transition = payment.markPaid("link-1", NOW.plusSeconds(5), NOW.plusSeconds(6));

        assertThat(transition).isEqualTo(PaidTransition.LATE_PAID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(payment.getPaidAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(payment.getFailureReason()).startsWith("Late PayOS success webhook");
    }
}
