package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.dto.PaymentDefaults;
import com.mv.paymentservice.application.exception.PaymentGatewayException;
import com.mv.paymentservice.application.port.in.ExpirePendingPaymentsUseCase;
import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Expires overdue PENDING payments. PayOS is asked to cancel the link first; if that fails its link state decides:
 * closed means expire, PAID means the webhook was lost and the payment becomes SUCCESS, anything else waits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpirePendingPaymentsService implements ExpirePendingPaymentsUseCase {

    static final String EXPIRE_REASON = "Payment deadline expired";

    private final LoadPaymentPort loadPaymentPort;
    private final PaymentSettlement settlement;
    private final PaymentDefaults paymentDefaults;
    private final Clock clock;

    @Override
    @Transactional
    public int expireDuePayments() {
        Instant now = clock.instant();
        List<Payment> due = loadPaymentPort.findDuePendingForUpdate(now, paymentDefaults.expirationBatchSize());
        int expired = 0;
        for (Payment payment : due) {
            try {
                GatewaySyncOutcome outcome = settlement.cancelAndClose(payment, PaymentStatus.EXPIRED, EXPIRE_REASON, now);
                if (outcome == GatewaySyncOutcome.CLOSED) {
                    expired++;
                } else if (outcome == GatewaySyncOutcome.PAID) {
                    log.warn("Overdue payment {} was in fact PAID on PayOS (webhook missed)", payment.getId());
                }
            } catch (PaymentGatewayException ex) {
                log.warn("Could not expire payment {} (orderCode {}): {}",
                        payment.getId(), payment.getPayosOrderCode(), ex.getMessage());
            }
        }
        return expired;
    }
}
