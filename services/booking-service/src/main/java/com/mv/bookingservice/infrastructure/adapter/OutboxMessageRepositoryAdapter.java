package com.mv.bookingservice.infrastructure.adapter;

import com.mv.bookingservice.application.port.out.OutboxMessagePort;
import com.mv.bookingservice.domain.model.outbox.OutboxMessage;
import com.mv.bookingservice.infrastructure.persistence.mapper.OutboxMessagePersistenceMapper;
import com.mv.bookingservice.infrastructure.persistence.repository.OutboxMessageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxMessageRepositoryAdapter implements OutboxMessagePort {

    private final OutboxMessageJpaRepository repository;
    private final OutboxMessagePersistenceMapper mapper;

    @Override
    public void save(OutboxMessage message) {
        repository.save(mapper.toInfraEntity(message));
    }
}
