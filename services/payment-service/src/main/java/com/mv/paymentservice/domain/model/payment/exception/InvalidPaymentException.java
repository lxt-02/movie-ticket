package com.mv.paymentservice.domain.model.payment.exception;

public class InvalidPaymentException extends DomainException {
    public InvalidPaymentException(String message) {
        super(message);
    }
}
