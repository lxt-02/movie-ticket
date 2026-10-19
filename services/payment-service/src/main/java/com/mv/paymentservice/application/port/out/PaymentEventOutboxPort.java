package com.mv.paymentservice.application.port.out;

import com.mv.paymentservice.application.event.PaymentEvent;

/**
 * Writes an event to the outbox in the caller's transaction; the relay publishes it to Kafka later.
 */
public interface PaymentEventOutboxPort {
    void append(PaymentEvent event);
}
