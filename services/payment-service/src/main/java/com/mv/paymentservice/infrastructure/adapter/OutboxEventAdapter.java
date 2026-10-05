package com.mv.paymentservice.infrastructure.adapter;

import com.mv.paymentservice.application.port.out.SaveOutboxEventPort;
import com.mv.paymentservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.mv.paymentservice.infrastructure.persistence.repository.OutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventAdapter implements SaveOutboxEventPort {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public void save(UUID aggregateId, String eventType, String payload) {
        OutboxEventEntity entity = OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .createdAt(Instant.now())
                .build();
        outboxEventJpaRepository.save(entity);
    }
}
