package com.mv.paymentservice.infrastructure.adapter;

import com.mv.paymentservice.application.event.PaymentEvent;
import com.mv.paymentservice.application.port.out.PaymentEventOutboxPort;
import com.mv.paymentservice.infrastructure.config.JsonCodec;
import com.mv.paymentservice.infrastructure.messaging.KafkaTopicsProperties;
import com.mv.paymentservice.infrastructure.persistence.entity.OutboxMessageEntity;
import com.mv.paymentservice.infrastructure.persistence.entity.OutboxStatus;
import com.mv.paymentservice.infrastructure.persistence.repository.OutboxMessageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores payment events in outbox_messages (same transaction as the business change).
 * Kafka key = bookingId so all events of one booking stay ordered on one partition.
 */
@Component
@RequiredArgsConstructor
public class OutboxMessageAdapter implements PaymentEventOutboxPort {

    static final String AGGREGATE_TYPE = "BOOKING";

    private final OutboxMessageJpaRepository repository;
    private final KafkaTopicsProperties topics;
    private final JsonCodec json;

    @Override
    public void append(PaymentEvent event) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", event.eventId());
        envelope.put("aggregateId", event.bookingId());
        envelope.put("eventType", event.eventType().name());
        envelope.put("occurredAt", event.occurredAt());
        envelope.put("payload", event.payload());

        repository.save(OutboxMessageEntity.builder()
                .id(event.eventId())
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(event.bookingId())
                .eventType(event.eventType().name())
                .topic(topics.paymentEvents())
                .messageKey(event.bookingId().toString())
                .payload(json.write(envelope))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(event.occurredAt())
                .build());
    }
}
