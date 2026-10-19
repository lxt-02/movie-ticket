package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.dto.PaymentLinkInfo;
import com.mv.paymentservice.application.dto.PaymentLinkTransaction;
import com.mv.paymentservice.application.event.PaymentEvent;
import com.mv.paymentservice.application.event.PaymentEventType;
import com.mv.paymentservice.application.port.out.PaymentEventOutboxPort;
import com.mv.paymentservice.application.port.out.PaymentGatewayPort;
import com.mv.paymentservice.application.port.out.SavePaymentPort;
import com.mv.paymentservice.application.port.out.SavePaymentTransactionPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.entity.PaymentTransaction;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reconcile = the lost-webhook safety net (job and /sync endpoint).
 */
@ExtendWith(MockitoExtension.class)
class PaymentSettlementTest {

    private static final Instant NOW = Instant.parse("2026-10-18T13:10:00Z");

    @Mock
    private RefundRequests refundRequests;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private SavePaymentPort savePaymentPort;
    @Mock
    private SavePaymentTransactionPort savePaymentTransactionPort;
    @Mock
    private PaymentEventOutboxPort outboxPort;

    private PaymentSettlement settlement;
    private Payment payment;

    @BeforeEach
    void setUp() {
        settlement = new PaymentSettlement(paymentGatewayPort, savePaymentPort, savePaymentTransactionPort, outboxPort, refundRequests);
        payment = Payment.createPending(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("150000"), "VND",
                "key", NOW.plusSeconds(300), NOW.minusSeconds(300));
        payment.attachPaymentLink("link-1", "https://pay.payos.vn/web/link-1", "qr", NOW.minusSeconds(300));
    }

    @Test
    void paidOnPayosBecomesSuccessWithBankReference() {
        Instant transferTime = NOW.minusSeconds(120);
        when(paymentGatewayPort.getPaymentLinkInfo(payment.getPayosOrderCode())).thenReturn(new PaymentLinkInfo(
                "paid", 150000, 150000, null, List.of(new PaymentLinkTransaction("FT555", 150000, transferTime)), "{}"));
        when(savePaymentTransactionPort.saveIfAbsent(any())).thenReturn(true);
        when(savePaymentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GatewaySyncOutcome outcome = settlement.reconcile(payment, NOW);

        assertThat(outcome).isEqualTo(GatewaySyncOutcome.PAID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaidAt()).isEqualTo(transferTime);
        ArgumentCaptor<PaymentTransaction> tx = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(savePaymentTransactionPort).saveIfAbsent(tx.capture());
        assertThat(tx.getValue().getTransactionCode()).isEqualTo("FT555");
        assertThat(tx.getValue().getGatewayTransactionId()).isEqualTo("link-1");
    }

    @Test
    void cancelledOnPayosPageBecomesCancelled() {
        when(paymentGatewayPort.getPaymentLinkInfo(payment.getPayosOrderCode())).thenReturn(new PaymentLinkInfo(
                "CANCELLED", 150000, 0, "Khách hủy", List.of(), "{}"));
        when(savePaymentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GatewaySyncOutcome outcome = settlement.reconcile(payment, NOW);

        assertThat(outcome).isEqualTo(GatewaySyncOutcome.CLOSED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        ArgumentCaptor<PaymentEvent> event = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(outboxPort).append(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(PaymentEventType.PAYMENT_CANCELLED);
    }

    @Test
    void paidWithWrongAmountIsLeftOpen() {
        when(paymentGatewayPort.getPaymentLinkInfo(payment.getPayosOrderCode())).thenReturn(new PaymentLinkInfo(
                "PAID", 150000, 100000, null, List.of(), "{}"));

        assertThat(settlement.reconcile(payment, NOW)).isEqualTo(GatewaySyncOutcome.STILL_OPEN);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(savePaymentPort, never()).save(any());
    }

    @Test
    void pendingOnPayosChangesNothing() {
        when(paymentGatewayPort.getPaymentLinkInfo(payment.getPayosOrderCode())).thenReturn(new PaymentLinkInfo(
                "PENDING", 150000, 0, null, List.of(), "{}"));

        assertThat(settlement.reconcile(payment, NOW)).isEqualTo(GatewaySyncOutcome.STILL_OPEN);
        verify(outboxPort, never()).append(any());
    }
}
