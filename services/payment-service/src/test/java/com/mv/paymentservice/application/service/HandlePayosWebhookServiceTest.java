package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.dto.PayosWebhook;
import com.mv.paymentservice.application.dto.PayosWebhookData;
import com.mv.paymentservice.application.event.PaymentEvent;
import com.mv.paymentservice.application.event.PaymentEventType;
import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.application.port.out.PaymentEventOutboxPort;
import com.mv.paymentservice.application.port.out.PaymentGatewayPort;
import com.mv.paymentservice.application.port.out.SavePaymentPort;
import com.mv.paymentservice.application.port.out.SavePaymentTransactionPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import com.mv.paymentservice.domain.model.payment.exception.InvalidWebhookException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandlePayosWebhookServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T13:05:00Z");
    private static final Instant TRANSFER_TIME = Instant.parse("2026-10-18T13:04:30Z");
    private static final String BODY = "{raw}";

    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private LoadPaymentPort loadPaymentPort;
    @Mock
    private SavePaymentPort savePaymentPort;
    @Mock
    private SavePaymentTransactionPort savePaymentTransactionPort;
    @Mock
    private PaymentEventOutboxPort outboxPort;

    private HandlePayosWebhookService service;
    private Payment payment;

    @BeforeEach
    void setUp() {
        PaymentSettlement settlement = new PaymentSettlement(paymentGatewayPort, savePaymentPort,
                savePaymentTransactionPort, outboxPort);
        service = new HandlePayosWebhookService(paymentGatewayPort, loadPaymentPort, settlement,
                Clock.fixed(NOW, ZoneOffset.UTC));
        payment = Payment.createPending(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("150000"), "VND",
                "key", NOW.plusSeconds(600), NOW.minusSeconds(60));
        payment.attachPaymentLink("link-1", "https://pay.payos.vn/web/link-1", "qr", NOW.minusSeconds(60));
    }

    private PayosWebhook webhook(boolean success, String code, long amount, String linkId) {
        PayosWebhookData data = new PayosWebhookData(payment.getPayosOrderCode(), amount, "VND", linkId,
                "FT123", code, success ? "success" : "failed", TRANSFER_TIME);
        return new PayosWebhook(code, success ? "success" : "failed", success, data, BODY);
    }

    @Test
    void successWebhookMarksPaidWithTransferTimeAndWritesEvent() {
        when(paymentGatewayPort.verifyWebhook(BODY)).thenReturn(webhook(true, "00", 150000, "link-1"));
        when(loadPaymentPort.findByPayosOrderCodeForUpdate(payment.getPayosOrderCode())).thenReturn(Optional.of(payment));
        when(savePaymentTransactionPort.saveIfAbsent(any())).thenReturn(true);
        when(savePaymentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handle(BODY);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaidAt()).isEqualTo(TRANSFER_TIME);
        ArgumentCaptor<PaymentEvent> event = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(outboxPort).append(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(PaymentEventType.PAYMENT_SUCCEEDED);
        assertThat(event.getValue().payload()).containsEntry("paymentId", payment.getId());
        assertThat(event.getValue().payload()).containsEntry("paidAt", TRANSFER_TIME);
    }

    @Test
    void duplicateSuccessWebhookDoesNothing() {
        payment.markPaid("link-1", TRANSFER_TIME, NOW.minusSeconds(10));
        when(paymentGatewayPort.verifyWebhook(BODY)).thenReturn(webhook(true, "00", 150000, "link-1"));
        when(loadPaymentPort.findByPayosOrderCodeForUpdate(payment.getPayosOrderCode())).thenReturn(Optional.of(payment));
        when(savePaymentTransactionPort.saveIfAbsent(any())).thenReturn(false);

        service.handle(BODY);

        verify(savePaymentPort, never()).save(any());
        verify(outboxPort, never()).append(any());
    }

    @Test
    void amountMismatchIsRejected() {
        when(paymentGatewayPort.verifyWebhook(BODY)).thenReturn(webhook(true, "00", 1000, "link-1"));
        when(loadPaymentPort.findByPayosOrderCodeForUpdate(payment.getPayosOrderCode())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.handle(BODY)).isInstanceOf(InvalidWebhookException.class);
        verify(savePaymentPort, never()).save(any());
        verify(outboxPort, never()).append(any());
    }

    @Test
    void paymentLinkIdMismatchIsRejected() {
        when(paymentGatewayPort.verifyWebhook(BODY)).thenReturn(webhook(true, "00", 150000, "other-link"));
        when(loadPaymentPort.findByPayosOrderCodeForUpdate(payment.getPayosOrderCode())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.handle(BODY)).isInstanceOf(InvalidWebhookException.class);
    }

    @Test
    void lateSuccessAfterExpiryKeepsStatusAndPublishesNothing() {
        payment.markExpired("Payment deadline expired", NOW.minusSeconds(5));
        when(paymentGatewayPort.verifyWebhook(BODY)).thenReturn(webhook(true, "00", 150000, "link-1"));
        when(loadPaymentPort.findByPayosOrderCodeForUpdate(payment.getPayosOrderCode())).thenReturn(Optional.of(payment));
        when(savePaymentTransactionPort.saveIfAbsent(any())).thenReturn(true);
        when(savePaymentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handle(BODY);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(payment.getFailureReason()).startsWith("Late PayOS success webhook");
        verify(savePaymentPort).save(payment);
        verify(outboxPort, never()).append(any());
    }

    @Test
    void unknownOrderCodeIsIgnored() {
        when(paymentGatewayPort.verifyWebhook(BODY)).thenReturn(webhook(true, "00", 150000, "link-1"));
        when(loadPaymentPort.findByPayosOrderCodeForUpdate(payment.getPayosOrderCode())).thenReturn(Optional.empty());

        service.handle(BODY);

        verify(savePaymentTransactionPort, never()).saveIfAbsent(any());
        verify(outboxPort, never()).append(any());
    }

    @Test
    void failedWebhookMarksFailed() {
        when(paymentGatewayPort.verifyWebhook(BODY)).thenReturn(webhook(false, "01", 150000, "link-1"));
        when(loadPaymentPort.findByPayosOrderCodeForUpdate(payment.getPayosOrderCode())).thenReturn(Optional.of(payment));
        when(savePaymentTransactionPort.saveIfAbsent(any())).thenReturn(true);
        when(savePaymentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handle(BODY);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        ArgumentCaptor<PaymentEvent> event = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(outboxPort).append(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(PaymentEventType.PAYMENT_FAILED);
    }
}
