package com.mv.movieservice.infrastructure.messaging;

import com.mv.movieservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.mv.movieservice.infrastructure.persistence.repository.OutboxEventJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
 * Publishes unpublished outbox_events to topic movie-events, oldest first, then sets published_at.
 * <p>
 * Several movie-service instances: each run locks the single row of {@code outbox_relay_lock} with
 * {@code FOR UPDATE SKIP LOCKED}; an instance that does not get it skips the run, so events are not sent twice
 * and the events of one movie keep their order.
 * <p>
 * At-least-once: a crash between the Kafka ack and the commit re-sends the event; search-service handles it
 * idempotently (upsert by movie id, versioned by updatedAt).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovieOutboxRelay {

    static final String LOCK_NAME = "movie-outbox";

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MovieKafkaProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;

    @Scheduled(
            fixedDelayString = "${movie.kafka.relay-delay-ms:2000}",
            initialDelayString = "${movie.kafka.relay-initial-delay-ms:5000}"
    )
    public void relay() {
        transactionTemplate.executeWithoutResult(status -> {
            if (!tryLock()) {
                log.debug("Movie outbox relay is running on another instance, skipping this run");
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
    void publishBatch() {
        List<OutboxEventEntity> pending = repository.findUnpublished(PageRequest.of(0, properties.relayBatchSize()));
        for (OutboxEventEntity event : pending) {
            try {
                kafkaTemplate.send(properties.movieEvents(), event.getAggregateId().toString(), event.getPayload())
                        .get(properties.publishTimeoutMs(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | TimeoutException | RuntimeException e) {
                // Keep the per-movie order: stop this run and retry on the next one.
                log.warn("Kafka publish failed for movie event {} ({}): {}", event.getId(), event.getEventType(), e.getMessage());
                return;
            }
            log.info("Kafka SEND topic={} key={} type={}", properties.movieEvents(), event.getAggregateId(), event.getEventType());
            event.setPublishedAt(Instant.now());
            repository.save(event);
        }
    }
}
