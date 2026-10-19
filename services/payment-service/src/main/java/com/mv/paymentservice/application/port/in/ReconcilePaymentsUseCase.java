package com.mv.paymentservice.application.port.in;

public interface ReconcilePaymentsUseCase {
    /**
     * Checks open PENDING payments against PayOS (covers lost webhooks and links cancelled on the PayOS page).
     *
     * @return number of payments whose status changed
     */
    int reconcileOpenPayments();
}
