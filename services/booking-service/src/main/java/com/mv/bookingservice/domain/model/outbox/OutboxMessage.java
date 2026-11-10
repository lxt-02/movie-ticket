package com.mv.bookingservice.domain.model.outbox;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * A message waiting in the transactional outbox (table outbox_messages). Written in the same transaction as the
 * business change; the outbox relay publishes it to {@link #topic} with key {@link #messageKey}.
 */
@Getter
public class OutboxMessage {

    private UUID id;
    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
    private String topic;
    private String messageKey;
    private String payload;
    private OutboxMessageStatus status;
    private int retryCount;
    private Instant nextRetryAt;
    private Instant createdAt;
    private Instant publishedAt;
    private String lastError;

    private OutboxMessage() {
    }

    public static OutboxMessage pending(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String topic,
            String messageKey,
            String payload
    ) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(messageKey, "messageKey must not be null");
        Objects.requireNonNull(payload, "payload must not be null");

        OutboxMessage message = new OutboxMessage();
        message.id = UUID.randomUUID();
        message.aggregateType = aggregateType;
        message.aggregateId = aggregateId;
        message.eventType = eventType;
        message.topic = topic;
        message.messageKey = messageKey;
        message.payload = payload;
        message.status = OutboxMessageStatus.PENDING;
        message.retryCount = 0;
        message.createdAt = Instant.now();
        return message;
    }
}
