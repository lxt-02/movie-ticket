package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.exception.PaymentGatewayException;
import com.mv.paymentservice.application.port.in.SyncPaymentUseCase;
import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncPaymentService implements SyncPaymentUseCase {

    private final LoadPaymentPort loadPaymentPort;
    private final PaymentSettlement settlement;
    private final Clock clock;

    @Override
    @Transactional
    public Payment syncPayment(UUID paymentId) {
        Payment payment = loadPaymentPort.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return reconcileQuietly(payment);
    }

    @Override
    @Transactional
    public Payment syncLatestPaymentOfBooking(UUID bookingId) {
        List<Payment> payments = loadPaymentPort.findByBookingIdForUpdate(bookingId);
        if (payments.isEmpty()) {
            throw new PaymentNotFoundException("No payment yet for booking " + bookingId);
        }
        return reconcileQuietly(payments.get(0));
    }

    private Payment reconcileQuietly(Payment payment) {
        if (!payment.getStatus().isTerminal()) {
            try {
                settlement.reconcile(payment, clock.instant());
            } catch (PaymentGatewayException ex) {
                // Return what we have; the reconcile job will try again.
                log.warn("Sync with PayOS failed for payment {}: {}", payment.getId(), ex.getMessage());
            }
        }
        return payment;
    }
}
