package com.mv.bookingservice.infrastructure.adapter;

import com.mv.bookingservice.application.port.out.OutboxPort;
import com.mv.bookingservice.domain.model.outbox.OutboxEvent;
import com.mv.bookingservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.mv.bookingservice.infrastructure.persistence.mapper.BookingPersistenceMapper;
import com.mv.bookingservice.infrastructure.persistence.repository.OutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRepositoryAdapter implements OutboxPort {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final BookingPersistenceMapper mapper;

    @Override
    public void save(OutboxEvent outboxEvent) {
        OutboxEventEntity entity = mapper.toOutboxEntity(outboxEvent);
        outboxEventJpaRepository.save(entity);
    }
}
