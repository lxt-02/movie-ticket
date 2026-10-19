package com.mv.paymentservice.infrastructure.scheduler;

import com.mv.paymentservice.application.port.in.ExpirePendingPaymentsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpirationScheduler {

    private final ExpirePendingPaymentsUseCase expirePendingPaymentsUseCase;

    @Scheduled(
            fixedDelayString = "${payment.expiration-scan-delay-ms:60000}",
            initialDelayString = "${payment.expiration-scan-initial-delay-ms:60000}"
    )
    public void expireOverduePayments() {
        try {
            int expired = expirePendingPaymentsUseCase.expireDuePayments();
            if (expired > 0) {
                log.info("Expired {} overdue payment(s)", expired);
            }
        } catch (RuntimeException e) {
            log.error("Payment expiration run failed", e);
        }
    }
}
