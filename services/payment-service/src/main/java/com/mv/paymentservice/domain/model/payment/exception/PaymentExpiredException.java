package com.mv.paymentservice.domain.model.payment.exception;

import java.util.UUID;

public class PaymentExpiredException extends DomainException {
    public PaymentExpiredException(UUID paymentId) {
        super("Payment " + paymentId + " has expired");
    }
}
