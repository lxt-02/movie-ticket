package com.mv.movieservice.domain.model.outbox;

public enum OutboxMessageStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
