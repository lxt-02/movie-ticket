package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.dto.PaymentLinkInfo;
import com.mv.paymentservice.application.dto.PaymentLinkTransaction;
import com.mv.paymentservice.application.event.PaymentEventFactory;
import com.mv.paymentservice.application.exception.PaymentGatewayException;
import com.mv.paymentservice.application.port.out.PaymentEventOutboxPort;
import com.mv.paymentservice.application.port.out.PaymentGatewayPort;
import com.mv.paymentservice.application.port.out.SavePaymentPort;
import com.mv.paymentservice.application.port.out.SavePaymentTransactionPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.entity.PaymentTransaction;
import com.mv.paymentservice.domain.model.payment.enums.PaidTransition;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import com.mv.paymentservice.domain.model.payment.enums.TransactionStatus;
import com.mv.paymentservice.domain.model.payment.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One place that applies PayOS results to a payment, shared by the webhook, the reconcile job, the sync endpoint,
 * the expiration job and payment retries. Callers must hold the payment row lock and run inside a transaction.
 * Every status change writes its event to the outbox in the same transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSettlement {

    private final PaymentGatewayPort paymentGatewayPort;
    private final SavePaymentPort savePaymentPort;
    private final SavePaymentTransactionPort savePaymentTransactionPort;
    private final PaymentEventOutboxPort outboxPort;

    /** Stores a gateway callback once. @return false if it was already stored. */
    public boolean recordTransaction(Payment payment, String reference, String paymentLinkId, boolean success,
                                     long amount, String rawJson, Instant now) {
        return savePaymentTransactionPort.saveIfAbsent(PaymentTransaction.builder()
                .id(UUID.randomUUID())
                .paymentId(payment.getId())
                .transactionCode(reference)
                .gatewayTransactionId(paymentLinkId)
                .transactionType(TransactionType.PAYMENT)
                .transactionStatus(success ? TransactionStatus.SUCCESS : TransactionStatus.FAILED)
                .amount(BigDecimal.valueOf(amount))
                .rawResponse(rawJson)
                .createdAt(now)
                .build());
    }

    public PaidTransition applyPaid(Payment payment, String paymentLinkId, Instant paidAt, Instant now) {
        PaidTransition transition = payment.markPaid(paymentLinkId, paidAt, now);
        switch (transition) {
            case MARKED_SUCCESS -> {
                Payment saved = savePaymentPort.save(payment);
                outboxPort.append(PaymentEventFactory.statusChanged(saved, now));
                log.info("Payment {} SUCCESS (booking {})", saved.getId(), saved.getBookingId());
            }
            case LATE_PAID -> {
                savePaymentPort.save(payment);
                // Money arrived for a closed link (booking cancelled / expired / replaced). No refund flow in this
                // system: the status stays as it is and the case is only logged for manual handling.
                log.warn("LATE PAID: payment {} (booking {}) received money while {}; handle manually",
                        payment.getId(), payment.getBookingId(), payment.getStatus());
            }
            case ALREADY_SUCCESS -> log.debug("Payment {} already SUCCESS", payment.getId());
        }
        return transition;
    }

    /** FAILED / EXPIRED / CANCELLED. @return true if the status changed (event written). */
    public boolean applyClosed(Payment payment, PaymentStatus target, String reason, Instant now) {
        if (!payment.close(target, reason, now)) {
            return false;
        }
        Payment saved = savePaymentPort.save(payment);
        outboxPort.append(PaymentEventFactory.statusChanged(saved, now));
        log.info("Payment {} {} ({})", saved.getId(), saved.getStatus(), reason);
        return true;
    }

    /**
     * Asks PayOS for the link state and applies it. Used when the webhook may have been lost.
     *
     * @throws PaymentGatewayException if PayOS cannot be reached
     */
    public GatewaySyncOutcome reconcile(Payment payment, Instant now) {
        if (payment.getStatus().isTerminal()) {
            return payment.getStatus() == PaymentStatus.SUCCESS ? GatewaySyncOutcome.PAID : GatewaySyncOutcome.CLOSED;
        }
        PaymentLinkInfo info = paymentGatewayPort.getPaymentLinkInfo(payment.getPayosOrderCode());
        if (info.isPaid()) {
            return applyPaidFromInfo(payment, info, now);
        }
        return switch (info.status()) {
            case "CANCELLED" -> close(payment, PaymentStatus.CANCELLED,
                    "Cancelled on PayOS" + (info.cancellationReason() != null ? ": " + info.cancellationReason() : ""), now);
            case "EXPIRED" -> close(payment, PaymentStatus.EXPIRED, "Payment link expired on PayOS", now);
            case "FAILED" -> close(payment, PaymentStatus.FAILED, "Payment failed on PayOS", now);
            default -> GatewaySyncOutcome.STILL_OPEN;
        };
    }

    /**
     * Cancels the PayOS link and moves the payment to {@code target}. If PayOS refuses the cancel, its link state
     * decides: already closed means close here too; PAID means the buyer paid in the meantime and the payment
     * becomes SUCCESS instead; anything else leaves the payment open.
     *
     * @throws PaymentGatewayException if PayOS cannot be reached at all
     */
    public GatewaySyncOutcome cancelAndClose(Payment payment, PaymentStatus target, String reason, Instant now) {
        if (payment.getStatus().isTerminal()) {
            return payment.getStatus() == PaymentStatus.SUCCESS ? GatewaySyncOutcome.PAID : GatewaySyncOutcome.CLOSED;
        }
        try {
            paymentGatewayPort.cancelPaymentLink(payment.getPayosOrderCode(), reason);
            return close(payment, target, reason, now);
        } catch (PaymentGatewayException cancelError) {
            PaymentLinkInfo info = paymentGatewayPort.getPaymentLinkInfo(payment.getPayosOrderCode());
            if (info.isPaid()) {
                return applyPaidFromInfo(payment, info, now);
            }
            if (info.isClosed()) {
                return close(payment, target, reason, now);
            }
            log.warn("Cannot close payment {}: cancel failed ({}) and PayOS status is {}",
                    payment.getId(), cancelError.getMessage(), info.status());
            return GatewaySyncOutcome.STILL_OPEN;
        }
    }

    private GatewaySyncOutcome close(Payment payment, PaymentStatus target, String reason, Instant now) {
        applyClosed(payment, target, reason, now);
        return GatewaySyncOutcome.CLOSED;
    }

    private GatewaySyncOutcome applyPaidFromInfo(Payment payment, PaymentLinkInfo info, Instant now) {
        if (info.amountPaid() != payment.amountAsLong()) {
            log.error("PayOS reports PAID for payment {} with amountPaid {} but expected {}; left for manual check",
                    payment.getId(), info.amountPaid(), payment.amountAsLong());
            return GatewaySyncOutcome.STILL_OPEN;
        }
        PaymentLinkTransaction transfer = info.latestTransaction();
        String reference = transfer != null && transfer.reference() != null && !transfer.reference().isBlank()
                ? transfer.reference()
                : "RECONCILE-" + payment.getPayosOrderCode();
        Instant paidAt = transfer != null ? transfer.transactionTime() : null;
        recordTransaction(payment, reference, payment.getGatewayTransactionId(), true, info.amountPaid(),
                info.rawJson(), now);
        applyPaid(payment, payment.getGatewayTransactionId(), paidAt, now);
        return GatewaySyncOutcome.PAID;
    }
}
