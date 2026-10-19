package com.mv.paymentservice.infrastructure.persistence.entity;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
