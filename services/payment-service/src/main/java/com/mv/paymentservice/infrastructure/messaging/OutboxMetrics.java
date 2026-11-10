package com.mv.paymentservice.infrastructure.messaging;

import com.mv.paymentservice.domain.model.outbox.OutboxMessageStatus;
import com.mv.paymentservice.infrastructure.persistence.repository.OutboxMessageJpaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Prometheus / Grafana: {@code payment_outbox_messages{status="pending|failed"}} and
 * {@code payment_outbox_oldest_pending_seconds}. Alert when failed > 0 or the backlog gets old.
 */
@Component
public class OutboxMetrics {

    public OutboxMetrics(MeterRegistry registry, OutboxMessageJpaRepository repository) {
        Gauge.builder("payment.outbox.messages", repository, r -> r.countByStatus(OutboxMessageStatus.PENDING.name()))
                .tag("status", "pending").description("Outbox messages not published yet").register(registry);
        Gauge.builder("payment.outbox.messages", repository, r -> r.countByStatus(OutboxMessageStatus.FAILED.name()))
                .tag("status", "failed").description("Outbox messages with a permanent error").register(registry);
        Gauge.builder("payment.outbox.oldest.pending.seconds", repository, r -> {
            Instant oldest = r.oldestCreatedAt(OutboxMessageStatus.PENDING.name());
            return oldest == null ? 0 : Duration.between(oldest, Instant.now()).toSeconds();
        }).description("Age of the oldest unpublished outbox message").register(registry);
    }
}
