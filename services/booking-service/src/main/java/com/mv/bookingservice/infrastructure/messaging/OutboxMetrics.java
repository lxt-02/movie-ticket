package com.mv.bookingservice.infrastructure.messaging;

import com.mv.bookingservice.domain.model.outbox.OutboxMessageStatus;
import com.mv.bookingservice.infrastructure.persistence.repository.OutboxMessageJpaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Prometheus / Grafana: {@code booking_outbox_messages{status="pending|failed"}} and
 * {@code booking_outbox_oldest_pending_seconds}. Alert when failed > 0 or the oldest pending row gets old
 * (broker down, wrong topic, missing permission).
 */
@Component
public class OutboxMetrics {

    public OutboxMetrics(MeterRegistry registry, OutboxMessageJpaRepository repository) {
        Gauge.builder("booking.outbox.messages", repository, r -> r.countByStatus(OutboxMessageStatus.PENDING.name()))
                .tag("status", "pending").description("Outbox rows not published yet").register(registry);
        Gauge.builder("booking.outbox.messages", repository, r -> r.countByStatus(OutboxMessageStatus.FAILED.name()))
                .tag("status", "failed").description("Outbox rows with a permanent error").register(registry);
        Gauge.builder("booking.outbox.oldest.pending.seconds", repository, r -> {
            Instant oldest = r.oldestCreatedAt(OutboxMessageStatus.PENDING.name());
            return oldest == null ? 0 : Duration.between(oldest, Instant.now()).toSeconds();
        }).description("Age of the oldest unpublished outbox row").register(registry);
    }
}
