package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.port.in.RequestRefundUseCase;
import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.application.port.out.SavePaymentPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * REFUND_PAYMENT: SUCCESS -> REFUND_PENDING plus a PENDING refund row. Idempotent: a payment already in
 * REFUND_PENDING / REFUNDED, or one whose money arrived late (refund already opened), only re-checks the row.
 * A payment that never received money is not refunded (logged).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestRefundService implements RequestRefundUseCase {

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final RefundRequests refundRequests;

    @Override
    @Transactional
    public boolean requestRefund(UUID bookingId, UUID paymentId, String reason) {
        if (paymentId == null) {
            log.error("REFUND_PAYMENT without paymentId for booking {} ignored: {}", bookingId, reason);
            return false;
        }
        Payment payment = loadPaymentPort.findByIdForUpdate(paymentId).orElse(null);
        if (payment == null) {
            log.error("REFUND_PAYMENT for unknown payment {} (booking {}) ignored: {}", paymentId, bookingId, reason);
            return false;
        }
        if (bookingId != null && !bookingId.equals(payment.getBookingId())) {
            log.error("REFUND_PAYMENT for payment {} names booking {} but the payment belongs to {}; ignored",
                    paymentId, bookingId, payment.getBookingId());
            return false;
        }

        String safeReason = reason == null || reason.isBlank() ? "Requested by booking-service" : reason;
        PaymentStatus status = payment.getStatus();
        if (status == PaymentStatus.SUCCESS) {
            payment.markRefundPending();
            savePaymentPort.save(payment);
            return refundRequests.open(payment, safeReason);
        }
        if (status == PaymentStatus.REFUND_PENDING || status == PaymentStatus.REFUNDED || payment.getPaidAt() != null) {
            return refundRequests.open(payment, safeReason);
        }
        log.warn("REFUND_PAYMENT for payment {} in status {} with no money received; nothing to refund",
                paymentId, status);
        return false;
    }
}
