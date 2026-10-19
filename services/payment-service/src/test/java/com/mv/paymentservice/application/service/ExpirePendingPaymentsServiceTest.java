package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.dto.PaymentDefaults;
import com.mv.paymentservice.application.dto.PaymentLinkInfo;
import com.mv.paymentservice.application.dto.PaymentLinkTransaction;
import com.mv.paymentservice.application.event.PaymentEvent;
import com.mv.paymentservice.application.event.PaymentEventType;
import com.mv.paymentservice.application.exception.PaymentGatewayException;
import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.application.port.out.PaymentEventOutboxPort;
import com.mv.paymentservice.application.port.out.PaymentGatewayPort;
import com.mv.paymentservice.application.port.out.SavePaymentPort;
import com.mv.paymentservice.application.port.out.SavePaymentTransactionPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpirePendingPaymentsServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T13:30:00Z");

    @Mock
    private RefundRequests refundRequests;
    @Mock
    private LoadPaymentPort loadPaymentPort;
    @Mock
    private SavePaymentPort savePaymentPort;
    @Mock
    private SavePaymentTransactionPort savePaymentTransactionPort;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private PaymentEventOutboxPort outboxPort;

    private ExpirePendingPaymentsService service;
    private Payment overdue;

    @BeforeEach
    void setUp() {
        PaymentSettlement settlement = new PaymentSettlement(paymentGatewayPort, savePaymentPort,
                savePaymentTransactionPort, outboxPort, refundRequests);
        service = new ExpirePendingPaymentsService(loadPaymentPort, settlement,
                new PaymentDefaults(Duration.ofMinutes(15), 100, Duration.ofSeconds(60), 50),
                Clock.fixed(NOW, ZoneOffset.UTC));
        Instant created = NOW.minusSeconds(1200);
        overdue = Payment.createPending(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("90000"), "VND",
                "key", created.plusSeconds(900), created);
        overdue.attachPaymentLink("link-1", "https://pay.payos.vn/web/link-1", "qr", created);
        when(loadPaymentPort.findDuePendingForUpdate(NOW, 100)).thenReturn(List.of(overdue));
    }

    private PaymentLinkInfo info(String status, long amountPaid, List<PaymentLinkTransaction> transfers) {
        return new PaymentLinkInfo(status, 90000, amountPaid, null, transfers, "{}");
    }

    @Test
    void cancelsOnPayosThenExpires() {
        when(savePaymentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int expired = service.expireDuePayments();

        assertThat(expired).isEqualTo(1);
        assertThat(overdue.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        verify(paymentGatewayPort).cancelPaymentLink(overdue.getPayosOrderCode(), ExpirePendingPaymentsService.EXPIRE_REASON);
        ArgumentCaptor<PaymentEvent> event = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(outboxPort).append(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(PaymentEventType.PAYMENT_EXPIRED);
    }

    @Test
    void lostWebhookIsRecoveredWhenPayosSaysPaid() {
        Instant transferTime = NOW.minusSeconds(400);
        doThrow(new PaymentGatewayException("cannot cancel"))
                .when(paymentGatewayPort).cancelPaymentLink(anyLong(), anyString());
        when(paymentGatewayPort.getPaymentLinkInfo(overdue.getPayosOrderCode())).thenReturn(
                info("PAID", 90000, List.of(new PaymentLinkTransaction("FT999", 90000, transferTime))));
        when(savePaymentTransactionPort.saveIfAbsent(any())).thenReturn(true);
        when(savePaymentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int expired = service.expireDuePayments();

        assertThat(expired).isZero();
        assertThat(overdue.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(overdue.getPaidAt()).isEqualTo(transferTime);
        ArgumentCaptor<PaymentEvent> event = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(outboxPort).append(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(PaymentEventType.PAYMENT_SUCCEEDED);
    }

    @Test
    void waitsWhilePayosIsStillProcessing() {
        doThrow(new PaymentGatewayException("cannot cancel"))
                .when(paymentGatewayPort).cancelPaymentLink(anyLong(), anyString());
        when(paymentGatewayPort.getPaymentLinkInfo(overdue.getPayosOrderCode())).thenReturn(info("PROCESSING", 0, List.of()));

        assertThat(service.expireDuePayments()).isZero();
        assertThat(overdue.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(savePaymentPort, never()).save(any());
        verify(outboxPort, never()).append(any());
    }

    @Test
    void expiresWhenCancelFailsButLinkAlreadyExpiredOnPayos() {
        doThrow(new PaymentGatewayException("already expired"))
                .when(paymentGatewayPort).cancelPaymentLink(anyLong(), anyString());
        when(paymentGatewayPort.getPaymentLinkInfo(overdue.getPayosOrderCode())).thenReturn(info("EXPIRED", 0, List.of()));
        when(savePaymentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.expireDuePayments()).isEqualTo(1);
        assertThat(overdue.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
    }
}
