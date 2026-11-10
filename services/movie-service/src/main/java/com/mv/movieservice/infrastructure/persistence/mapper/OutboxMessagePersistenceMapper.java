package com.mv.movieservice.infrastructure.persistence.mapper;

import com.mv.movieservice.domain.model.outbox.OutboxMessage;
import com.mv.movieservice.infrastructure.persistence.entity.OutboxMessageInfraEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxMessagePersistenceMapper {

    public OutboxMessageInfraEntity toInfraEntity(OutboxMessage message) {
        if (message == null) {
            return null;
        }
        return OutboxMessageInfraEntity.builder()
                .id(message.getId())
                .aggregateType(message.getAggregateType())
                .aggregateId(message.getAggregateId())
                .eventType(message.getEventType())
                .topic(message.getTopic())
                .messageKey(message.getMessageKey())
                .payload(message.getPayload())
                .status(message.getStatus().name())
                .retryCount(message.getRetryCount())
                .nextRetryAt(message.getNextRetryAt())
                .createdAt(message.getCreatedAt())
                .publishedAt(message.getPublishedAt())
                .lastError(message.getLastError())
                .build();
    }
}
