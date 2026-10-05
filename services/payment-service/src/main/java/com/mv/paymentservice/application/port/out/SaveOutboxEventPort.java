package com.mv.paymentservice.application.port.out;

import java.util.UUID;

public interface SaveOutboxEventPort {
    void save(UUID aggregateId, String eventType, String payload);
}
