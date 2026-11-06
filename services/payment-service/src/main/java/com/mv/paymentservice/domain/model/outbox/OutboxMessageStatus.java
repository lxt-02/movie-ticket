package com.mv.paymentservice.domain.model.outbox;

public enum OutboxMessageStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
