package com.mv.paymentservice.infrastructure.scheduler;

import com.mv.paymentservice.application.port.in.ReconcilePaymentsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Safety net for lost webhooks: checks open PENDING payments against PayOS.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconcileScheduler {

    private final ReconcilePaymentsUseCase reconcilePaymentsUseCase;

    @Scheduled(
            fixedDelayString = "${payment.reconcile.delay-ms:60000}",
            initialDelayString = "${payment.reconcile.initial-delay-ms:30000}"
    )
    public void reconcile() {
        try {
            int changed = reconcilePaymentsUseCase.reconcileOpenPayments();
            if (changed > 0) {
                log.info("Reconciled {} payment(s) with PayOS", changed);
            }
        } catch (RuntimeException e) {
            log.error("Payment reconcile run failed", e);
        }
    }
}
