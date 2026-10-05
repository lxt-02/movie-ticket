package com.mv.bookingservice.domain.model.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    private UUID id;
    private UUID aggregateId;
    private String eventType;
    private String payload;
    private Instant publishedAt;
    private Instant createdAt;

    public static OutboxEvent create(UUID aggregateId, String eventType, String payload) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .createdAt(Instant.now())
                .build();
    }
}
