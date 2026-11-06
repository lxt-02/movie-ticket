package com.mv.paymentservice.application.port.out;

import com.mv.paymentservice.domain.model.outbox.OutboxMessage;

/** Stores an outbox message in the caller's transaction; the outbox relay publishes it to Kafka later. */
public interface OutboxMessagePort {
    void save(OutboxMessage message);
}
