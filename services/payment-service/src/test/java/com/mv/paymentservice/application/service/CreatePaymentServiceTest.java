package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.command.CreatePaymentCommand;
import com.mv.paymentservice.application.dto.PaymentDefaults;
import com.mv.paymentservice.application.dto.PaymentLink;
import com.mv.paymentservice.application.dto.PaymentLinkInfo;
import com.mv.paymentservice.application.dto.PaymentLinkRequest;
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
import com.mv.paymentservice.domain.model.payment.exception.InvalidPaymentException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T13:00:00Z");

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

    private CreatePaymentService service;
    private UUID bookingId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        PaymentSettlement settlement = new PaymentSettlement(paymentGatewayPort, savePaymentPort,
                savePaymentTransactionPort, outboxPort);
        service = new CreatePaymentService(loadPaymentPort, savePaymentPort, paymentGatewayPort, outboxPort, settlement,
                new PaymentDefaults(Duration.ofMinutes(15), 100, Duration.ofSeconds(60), 50),
                Clock.fixed(NOW, ZoneOffset.UTC));
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private CreatePaymentCommand command(String key) {
        return CreatePaymentCommand.builder()
                .commandId(UUID.randomUUID())
                .bookingId(bookingId)
                .userId(userId)
                .amount(new BigDecimal("150000.00"))
                .currency("VND")
                .provider("PAYOS")
                .expiredAt(NOW.plusSeconds(600))
                .idempotencyKey(key)
                .build();
    }

    private Payment openPayment(String key) {
        Payment payment = Payment.createPending(bookingId, userId, new BigDecimal("150000"), "VND",
                key, NOW.plusSeconds(600), NOW.minusSeconds(30));
        payment.attachPaymentLink("old-link", "https://pay.payos.vn/web/old-link", "qr", NOW.minusSeconds(30));
        return payment;
    }

    private void newLinkWillBeCreated() {
        when(paymentGatewayPort.createPaymentLink(any())).thenReturn(
                new PaymentLink("link-new", "https://pay.payos.vn/web/link-new", "000201..."));
        when(savePaymentPort.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createsPayosLinkAndWritesLinkCreated() {
        String key = "payment:create:PAYOS:" + bookingId + ":1";
        when(loadPaymentPort.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(loadPaymentPort.findByBookingIdForUpdate(bookingId)).thenReturn(List.of());
        newLinkWillBeCreated();

        Payment payment = service.createPayment(command(key));

        ArgumentCaptor<PaymentLinkRequest> request = ArgumentCaptor.forClass(PaymentLinkRequest.class);
        verify(paymentGatewayPort).createPaymentLink(request.capture());
        assertThat(request.getValue().amount()).isEqualTo(150000L);
        assertThat(request.getValue().orderCode()).isEqualTo(payment.getPayosOrderCode());
        assertThat(request.getValue().expiredAt()).isEqualTo(NOW.plusSeconds(600));
        assertThat(request.getValue().description()).startsWith("SP").hasSize(9);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getIdempotencyKey()).isEqualTo(key);
        assertThat(payment.getGatewayTransactionId()).isEqualTo("link-new");

        ArgumentCaptor<PaymentEvent> event = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(outboxPort).append(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(PaymentEventType.PAYMENT_LINK_CREATED);
        assertThat(event.getValue().bookingId()).isEqualTo(bookingId);
    }

    @Test
    void sameIdempotencyKeyRepublishesCurrentStateWithoutCallingPayos() {
        String key = "payment:create:PAYOS:" + bookingId + ":1";
        Payment existing = openPayment(key);
        when(loadPaymentPort.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));

        Payment result = service.createPayment(command(key));

        assertThat(result).isSameAs(existing);
        verify(paymentGatewayPort, never()).createPaymentLink(any());
        ArgumentCaptor<PaymentEvent> event = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(outboxPort).append(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(PaymentEventType.PAYMENT_LINK_CREATED);
    }

    @Test
    void retryCancelsOldLinkThenCreatesNewOne() {
        Payment old = openPayment("attempt-1");
        when(loadPaymentPort.findByIdempotencyKey("attempt-2")).thenReturn(Optional.empty());
        when(loadPaymentPort.findByBookingIdForUpdate(bookingId)).thenReturn(List.of(old));
        newLinkWillBeCreated();

        Payment result = service.createPayment(command("attempt-2"));

        verify(paymentGatewayPort).cancelPaymentLink(old.getPayosOrderCode(), CreatePaymentService.REPLACED_REASON);
        assertThat(old.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(result).isNotSameAs(old);
        assertThat(result.getGatewayTransactionId()).isEqualTo("link-new");

        ArgumentCaptor<PaymentEvent> events = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(outboxPort, times(2)).append(events.capture());
        assertThat(events.getAllValues()).extracting(PaymentEvent::eventType)
                .containsExactly(PaymentEventType.PAYMENT_CANCELLED, PaymentEventType.PAYMENT_LINK_CREATED);
    }

    @Test
    void retryDoesNotOpenSecondLinkWhenOldOneWasPaid() {
        Payment old = openPayment("attempt-1");
        when(loadPaymentPort.findByIdempotencyKey("attempt-2")).thenReturn(Optional.empty());
        when(loadPaymentPort.findByBookingIdForUpdate(bookingId)).thenReturn(List.of(old));
        doThrow(new PaymentGatewayException("already paid"))
                .when(paymentGatewayPort).cancelPaymentLink(anyLong(), anyString());
        when(paymentGatewayPort.getPaymentLinkInfo(old.getPayosOrderCode()))
                .thenReturn(new PaymentLinkInfo("PAID", 150000, 150000, null, List.of(), "{}"));
        when(savePaymentTransactionPort.saveIfAbsent(any())).thenReturn(true);
        when(savePaymentPort.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.createPayment(command("attempt-2"));

        assertThat(result).isSameAs(old);
        assertThat(old.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentGatewayPort, never()).createPaymentLink(any());
    }

    @Test
    void bookingAlreadyPaidRepublishesSuccess() {
        Payment paid = openPayment("attempt-1");
        paid.markPaid("old-link", NOW.minusSeconds(10), NOW.minusSeconds(5));
        when(loadPaymentPort.findByIdempotencyKey("attempt-2")).thenReturn(Optional.empty());
        when(loadPaymentPort.findByBookingIdForUpdate(bookingId)).thenReturn(List.of(paid));

        Payment result = service.createPayment(command("attempt-2"));

        assertThat(result).isSameAs(paid);
        verify(paymentGatewayPort, never()).createPaymentLink(any());
        ArgumentCaptor<PaymentEvent> event = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(outboxPort).append(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(PaymentEventType.PAYMENT_SUCCEEDED);
    }

    @Test
    void rejectsFractionalAmountAndOtherProviders() {
        CreatePaymentCommand fractional = command(null);
        fractional.setAmount(new BigDecimal("1000.50"));
        assertThatThrownBy(() -> service.createPayment(fractional)).isInstanceOf(InvalidPaymentException.class);

        CreatePaymentCommand momo = command(null);
        momo.setProvider("MOMO");
        assertThatThrownBy(() -> service.createPayment(momo)).isInstanceOf(InvalidPaymentException.class);

        verify(paymentGatewayPort, never()).createPaymentLink(any());
    }

    @Test
    void rejectsDeadlineInThePast() {
        CreatePaymentCommand cmd = command(null);
        cmd.setExpiredAt(NOW.minusSeconds(1));
        when(loadPaymentPort.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(loadPaymentPort.findByBookingIdForUpdate(bookingId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.createPayment(cmd)).isInstanceOf(InvalidPaymentException.class);
        verify(paymentGatewayPort, never()).createPaymentLink(any());
    }

    @Test
    void transferDescriptionUsesLastSevenDigits() {
        assertThat(CreatePaymentService.transferDescription(1234567891234L)).isEqualTo("SP7891234");
    }
}
