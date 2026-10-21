package com.mv.paymentservice.domain.model.payment.enums;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    EXPIRED,
    CANCELLED;

    public boolean isTerminal() {
        return this != PENDING && this != PROCESSING;
    }
}
