package com.mv.bookingservice.infrastructure.messaging;

import com.mv.bookingservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.mv.bookingservice.infrastructure.persistence.repository.OutboxEventJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes unpublished outbox_events to Kafka, oldest first, then sets published_at.
 * At-least-once: a crash between send and update re-sends the command, which payment-service handles
 * idempotently (same idempotencyKey).
 */
@Slf4j
@Component
public class BookingOutboxRelay {

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BookingKafkaProperties properties;

    public BookingOutboxRelay(OutboxEventJpaRepository repository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              BookingKafkaProperties properties) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${booking.kafka.relay-delay-ms:2000}",
            initialDelayString = "${booking.kafka.relay-initial-delay-ms:3000}"
    )
    public void relay() {
        List<OutboxEventEntity> pending = repository.findByPublishedAtIsNullOrderByCreatedAtAsc();
        int sent = 0;
        for (OutboxEventEntity event : pending) {
            if (sent >= properties.relayBatchSize()) {
                break;
            }
            String topic = topicFor(event.getEventType());
            if (topic == null) {
                log.warn("No Kafka topic for outbox event type {}, leaving {} unpublished", event.getEventType(), event.getId());
                continue;
            }
            try {
                kafkaTemplate.send(topic, event.getAggregateId().toString(), event.getPayload())
                        .get(properties.publishTimeoutMs(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | TimeoutException | RuntimeException e) {
                // Keep order per booking: stop this run and retry on the next one.
                log.warn("Kafka publish failed for outbox event {} ({}): {}", event.getId(), event.getEventType(), e.getMessage());
                return;
            }
            log.info("Kafka SEND topic={} key={} type={} body={}",
                    topic, event.getAggregateId(), event.getEventType(), event.getPayload());
            event.setPublishedAt(Instant.now());
            repository.save(event);
            sent++;
        }
    }

    private String topicFor(String eventType) {
        if (PaymentCommandOutboxAdapter.CREATE_PAYMENT.equals(eventType)
                || PaymentCommandOutboxAdapter.CANCEL_PAYMENT.equals(eventType)
                || PaymentCommandOutboxAdapter.REFUND_PAYMENT.equals(eventType)) {
            return properties.paymentCommands();
        }
        return null;
    }
}
