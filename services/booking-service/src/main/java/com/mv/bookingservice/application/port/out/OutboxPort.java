package com.mv.bookingservice.application.port.out;

import com.mv.bookingservice.domain.model.outbox.OutboxEvent;

public interface OutboxPort {
    void save(OutboxEvent outboxEvent);
}
