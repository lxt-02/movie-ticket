package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.exception.PaymentGatewayException;
import com.mv.paymentservice.application.port.in.CancelBookingPaymentsUseCase;
import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * CANCEL_PAYMENT (booking cancelled or expired): close the open PayOS links so the buyer cannot pay any more.
 * A link that turns out to be PAID becomes SUCCESS (booking-service then logs it for a refund).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelBookingPaymentsService implements CancelBookingPaymentsUseCase {

    private final LoadPaymentPort loadPaymentPort;
    private final PaymentSettlement settlement;
    private final Clock clock;

    @Override
    @Transactional
    public int cancelOpenPayments(UUID bookingId, String reason) {
        Instant now = clock.instant();
        String safeReason = reason == null || reason.isBlank() ? "Booking closed" : reason;
        int cancelled = 0;
        for (Payment payment : loadPaymentPort.findByBookingIdForUpdate(bookingId)) {
            if (payment.getStatus().isTerminal()) {
                continue;
            }
            try {
                if (settlement.cancelAndClose(payment, PaymentStatus.CANCELLED, safeReason, now) == GatewaySyncOutcome.CLOSED) {
                    cancelled++;
                }
            } catch (PaymentGatewayException ex) {
                // The link still expires on PayOS at its deadline; the expiration job closes it here.
                log.warn("Could not cancel payment {} of booking {}: {}", payment.getId(), bookingId, ex.getMessage());
            }
        }
        return cancelled;
    }
}
