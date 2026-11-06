package com.mv.paymentservice.infrastructure.adapter;

import com.mv.paymentservice.application.port.out.OutboxMessagePort;
import com.mv.paymentservice.domain.model.outbox.OutboxMessage;
import com.mv.paymentservice.infrastructure.persistence.mapper.OutboxMessagePersistenceMapper;
import com.mv.paymentservice.infrastructure.persistence.repository.OutboxMessageJpaRepository;
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
