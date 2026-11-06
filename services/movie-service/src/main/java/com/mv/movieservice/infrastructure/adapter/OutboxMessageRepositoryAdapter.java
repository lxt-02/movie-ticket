package com.mv.movieservice.infrastructure.adapter;

import com.mv.movieservice.application.port.out.OutboxMessagePort;
import com.mv.movieservice.domain.model.outbox.OutboxMessage;
import com.mv.movieservice.infrastructure.persistence.mapper.OutboxMessagePersistenceMapper;
import com.mv.movieservice.infrastructure.persistence.repository.OutboxMessageJpaRepository;
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
