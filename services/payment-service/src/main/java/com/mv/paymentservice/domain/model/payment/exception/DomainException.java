package com.mv.paymentservice.domain.model.payment.exception;

public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
