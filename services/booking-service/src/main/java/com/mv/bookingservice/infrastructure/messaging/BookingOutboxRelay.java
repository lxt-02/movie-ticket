package com.mv.bookingservice.infrastructure.messaging;

import com.mv.bookingservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.mv.bookingservice.infrastructure.persistence.repository.OutboxEventJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes unpublished outbox_events to Kafka, oldest first, then sets published_at.
 * <p>
 * <b>Several booking-service instances</b> (load balancing): every instance runs this job, but only one may publish
 * at a time, otherwise the same command is sent twice and CREATE / CANCEL of one booking can overtake each other.
 * Each run opens a transaction and locks the single row of {@code outbox_relay_lock} with
 * {@code FOR UPDATE SKIP LOCKED}; an instance that does not get it skips the run (no waiting, no deadlock). If the
 * active instance dies, its lock is released with its connection and another instance takes over on its next run.
 * <p>
 * At-least-once: a crash between the Kafka ack and the commit re-sends the command, which payment-service handles
 * idempotently (same idempotencyKey / commandId).
 */
@Slf4j
@Component
public class BookingOutboxRelay {

    static final String LOCK_NAME = "booking-outbox";

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BookingKafkaProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;

    public BookingOutboxRelay(OutboxEventJpaRepository repository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              BookingKafkaProperties properties,
                              TransactionTemplate transactionTemplate,
                              EntityManager entityManager) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
        this.entityManager = entityManager;
    }

    @Scheduled(
            fixedDelayString = "${booking.kafka.relay-delay-ms:2000}",
            initialDelayString = "${booking.kafka.relay-initial-delay-ms:3000}"
    )
    public void relay() {
        transactionTemplate.executeWithoutResult(status -> {
            if (!tryLock()) {
                log.debug("Outbox relay is running on another instance, skipping this run");
                return;
            }
            publishBatch();
        });
    }

    /** Row lock held until the transaction ends. */
    private boolean tryLock() {
        List<?> locked = entityManager
                .createNativeQuery("SELECT name FROM outbox_relay_lock WHERE name = :name FOR UPDATE SKIP LOCKED")
                .setParameter("name", LOCK_NAME)
                .getResultList();
        return !locked.isEmpty();
    }

    /** Never throws: rows already sent keep their published_at when the transaction commits. */
    private void publishBatch() {
        List<OutboxEventEntity> pending = repository.findUnpublished(properties.relayBatchSize());
        for (OutboxEventEntity event : pending) {
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
        }
    }

    private String topicFor(String eventType) {
        if (PaymentCommandOutboxAdapter.CREATE_PAYMENT.equals(eventType)
                || PaymentCommandOutboxAdapter.CANCEL_PAYMENT.equals(eventType)) {
            return properties.paymentCommands();
        }
        return null;
    }
}
