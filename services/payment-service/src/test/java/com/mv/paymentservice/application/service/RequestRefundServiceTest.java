package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.application.port.out.SavePaymentPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import com.mv.paymentservice.domain.model.refund.aggregate.Refund;
import com.mv.paymentservice.domain.repository.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestRefundServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");

    @Mock
    private LoadPaymentPort loadPaymentPort;
    @Mock
    private SavePaymentPort savePaymentPort;
    @Mock
    private RefundRepository refundRepository;

    private RequestRefundService service;
    private Payment payment;

    @BeforeEach
    void setUp() {
        service = new RequestRefundService(loadPaymentPort, savePaymentPort, new RefundRequests(refundRepository));
        payment = Payment.createPending(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("150000"), "VND",
                "key-1", NOW.plusSeconds(600), NOW);
        when(loadPaymentPort.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
    }

    @Test
    void successfulPaymentBecomesRefundPendingWithOneRefundRow() {
        payment.markPaid("link-1", NOW, NOW);
        when(refundRepository.findByPaymentIdAndIdempotencyKey(any(), anyString())).thenReturn(Optional.empty());

        assertThat(service.requestRefund(payment.getBookingId(), payment.getId(), "Seats lost")).isTrue();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        ArgumentCaptor<Refund> refund = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(refund.capture());
        assertThat(refund.getValue().getAmount()).isEqualByComparingTo("150000");
        assertThat(refund.getValue().getIdempotencyKey()).isEqualTo("refund:" + payment.getId());
    }

    @Test
    void repeatedRequestDoesNotCreateASecondRefund() {
        payment.markPaid("link-1", NOW, NOW);
        payment.markRefundPending();
        when(refundRepository.findByPaymentIdAndIdempotencyKey(any(), anyString()))
                .thenReturn(Optional.of(Refund.builder().build()));

        assertThat(service.requestRefund(payment.getBookingId(), payment.getId(), "again")).isFalse();
        verify(refundRepository, never()).save(any());
    }

    @Test
    void unpaidPaymentIsNotRefunded() {
        assertThat(service.requestRefund(payment.getBookingId(), payment.getId(), "nothing paid")).isFalse();
        verify(refundRepository, never()).save(any());
        verify(savePaymentPort, never()).save(any());
    }

    @Test
    void paymentOfAnotherBookingIsIgnored() {
        assertThat(service.requestRefund(UUID.randomUUID(), payment.getId(), "wrong booking")).isFalse();
        verify(refundRepository, never()).save(any());
    }
}
