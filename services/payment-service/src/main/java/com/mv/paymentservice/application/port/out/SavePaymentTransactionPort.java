package com.mv.paymentservice.application.port.out;

import com.mv.paymentservice.domain.model.payment.entity.PaymentTransaction;

public interface SavePaymentTransactionPort {
    /**
     * Inserts the transaction unless one with the same (paymentId, transactionCode, gatewayTransactionId) exists.
     *
     * @return true if inserted, false if it was a duplicate
     */
    boolean saveIfAbsent(PaymentTransaction transaction);
}
