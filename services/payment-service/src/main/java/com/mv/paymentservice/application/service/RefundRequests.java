package com.mv.paymentservice.application.service;

import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.refund.aggregate.Refund;
import com.mv.paymentservice.domain.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Opens the refund to-do for money that must go back to the buyer: one {@code refunds} row (status PENDING) per
 * payment, keyed {@code refund:{paymentId}}, so repeated triggers (redelivered command, duplicate webhook) never
 * create a second one. PayOS transfers are refunded by staff; the PENDING rows are their work list.
 * Callers run inside a transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundRequests {

    static final String KEY_PREFIX = "refund:";

    private final RefundRepository refundRepository;

    /** @return true if a new refund row was created, false if the payment already had one. */
    public boolean open(Payment payment, String reason) {
        String key = KEY_PREFIX + payment.getId();
        if (refundRepository.findByPaymentIdAndIdempotencyKey(payment.getId(), key).isPresent()) {
            log.info("Refund for payment {} already requested", payment.getId());
            return false;
        }
        Refund refund = Refund.create(UUID.randomUUID(), payment.getId(), payment.getAmount(), reason,
                key, payment.getId().toString().replace("-", ""));
        refundRepository.save(refund);
        log.error("REFUND REQUESTED: payment {} of booking {} ({} {}): {}",
                payment.getId(), payment.getBookingId(), payment.getAmount(), payment.getCurrency(), reason);
        return true;
    }
}
