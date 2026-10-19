package com.mv.paymentservice.application.port.in;

public interface ExpirePendingPaymentsUseCase {
    /** @return number of payments moved to EXPIRED */
    int expireDuePayments();
}
