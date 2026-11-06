package com.mv.bookingservice.application.port.out;

import com.mv.bookingservice.domain.model.outbox.OutboxMessage;

/** Stores an outbox message in the caller's transaction; the outbox relay publishes it to Kafka later. */
public interface OutboxMessagePort {
    void save(OutboxMessage message);
}
