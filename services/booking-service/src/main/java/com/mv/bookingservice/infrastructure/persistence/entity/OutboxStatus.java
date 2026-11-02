package com.mv.bookingservice.infrastructure.persistence.entity;

/** Delivery state of an outbox_events row (see V3__outbox_reliability.sql). */
public enum OutboxStatus {
    /** Not sent yet, or a temporary error: retried with back-off. */
    PENDING,
    /** Acknowledged by the broker. */
    PUBLISHED,
    /** Permanent error: waits for an operator (requeue) instead of blocking every booking. */
    FAILED
}
