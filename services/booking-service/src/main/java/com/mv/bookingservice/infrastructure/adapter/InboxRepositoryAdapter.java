package com.mv.bookingservice.infrastructure.adapter;

import com.mv.bookingservice.application.port.out.InboxPort;
import com.mv.bookingservice.infrastructure.persistence.entity.InboxEventEntity;
import com.mv.bookingservice.infrastructure.persistence.repository.InboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InboxRepositoryAdapter implements InboxPort {

    private final InboxEventJpaRepository repository;

    @Override
    public boolean isProcessed(UUID eventId) {
        return repository.existsById(eventId);
    }

    @Override
    public void markProcessed(UUID eventId, String eventType) {
        repository.save(InboxEventEntity.builder()
                .eventId(eventId)
                .eventType(eventType == null ? "UNKNOWN" : eventType)
                .processedAt(Instant.now())
                .build());
    }
}
