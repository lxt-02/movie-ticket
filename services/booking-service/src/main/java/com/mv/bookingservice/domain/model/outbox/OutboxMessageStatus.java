package com.mv.bookingservice.domain.model.outbox;

public enum OutboxMessageStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
