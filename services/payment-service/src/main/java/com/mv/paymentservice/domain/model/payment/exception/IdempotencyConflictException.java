package com.mv.paymentservice.domain.model.payment.exception;

public class IdempotencyConflictException extends DomainException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
