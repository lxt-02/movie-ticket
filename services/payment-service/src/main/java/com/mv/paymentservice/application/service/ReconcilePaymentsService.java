package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.dto.PaymentDefaults;
import com.mv.paymentservice.application.exception.PaymentGatewayException;
import com.mv.paymentservice.application.port.in.ReconcilePaymentsUseCase;
import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Safety net for lost webhooks: PENDING payments older than payment.reconcile.min-age are checked against PayOS.
 * PAID becomes SUCCESS, a link cancelled on the PayOS page becomes CANCELLED. Overdue payments are left to
 * the expiration job.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconcilePaymentsService implements ReconcilePaymentsUseCase {

    private final LoadPaymentPort loadPaymentPort;
    private final PaymentSettlement settlement;
    private final PaymentDefaults paymentDefaults;
    private final Clock clock;

    @Override
    @Transactional
    public int reconcileOpenPayments() {
        Instant now = clock.instant();
        List<Payment> open = loadPaymentPort.findOpenForReconcileForUpdate(
                now.minus(paymentDefaults.reconcileMinAge()), now, paymentDefaults.reconcileBatchSize());
        int changed = 0;
        for (Payment payment : open) {
            try {
                GatewaySyncOutcome outcome = settlement.reconcile(payment, now);
                if (outcome != GatewaySyncOutcome.STILL_OPEN) {
                    changed++;
                    log.info("Reconciled payment {} with PayOS: {}", payment.getId(), outcome);
                }
            } catch (PaymentGatewayException ex) {
                log.warn("Could not reconcile payment {}: {}", payment.getId(), ex.getMessage());
            }
        }
        return changed;
    }
}
