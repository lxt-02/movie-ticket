package com.mv.paymentservice.domain.model.payment.exception;

import java.util.UUID;

public class PaymentNotFoundException extends DomainException {
    public PaymentNotFoundException(UUID paymentId) {
        super("Payment not found with id: " + paymentId);
    }
}
