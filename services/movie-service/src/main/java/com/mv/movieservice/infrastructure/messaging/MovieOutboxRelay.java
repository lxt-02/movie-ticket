package com.mv.movieservice.infrastructure.messaging;

import com.mv.movieservice.domain.model.outbox.OutboxMessageStatus;
import com.mv.movieservice.infrastructure.persistence.entity.OutboxMessageInfraEntity;
import com.mv.movieservice.infrastructure.persistence.repository.OutboxMessageJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publishes PENDING outbox_messages to their topic (movie-events) with their key (movieId), oldest first, then
 * marks them PUBLISHED.
 * <p>
 * Several movie-service instances: each run locks the single row of {@code outbox_relay_lock} with
 * {@code FOR UPDATE SKIP LOCKED}; an instance that does not get it skips the run, so events are not sent twice
 * and the events of one movie keep their order.
 * <p>
 * Publish error: the row stays PENDING with {@code retry_count}, {@code last_error} and a back-off
 * ({@code next_retry_at}: 2s, 4s, 8s ... max 60s). The run stops there (order kept); nothing is published until
 * that row is retried.
 * <p>
 * At-least-once: a crash between the Kafka ack and the commit re-sends the event; search-service handles it
 * idempotently (upsert by movie id, versioned by updatedAt).
 */
@Slf4j
@Component
public class MovieOutboxRelay {

    static final String LOCK_NAME = "movie-outbox";
    static final int MAX_ERROR_LENGTH = 2000;
    static final Duration RETRY_INITIAL_DELAY = Duration.ofSeconds(2);
    static final Duration RETRY_MAX_DELAY = Duration.ofSeconds(60);

    private final OutboxMessageJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MovieKafkaProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final Clock clock;

    @Autowired
    public MovieOutboxRelay(OutboxMessageJpaRepository repository,
                            KafkaTemplate<String, String> kafkaTemplate,
                            MovieKafkaProperties properties,
                            TransactionTemplate transactionTemplate,
                            EntityManager entityManager,
                            Clock clock) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
        this.entityManager = entityManager;
        this.clock = clock;
    }

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

    /** Never throws: the outcome of every attempted row is saved when the transaction commits. */
    void publishBatch() {
        Instant now = clock.instant();
        List<OutboxMessageInfraEntity> pending = repository.findPending(PageRequest.of(0, properties.relayBatchSize()));
        for (OutboxMessageInfraEntity message : pending) {
            if (message.getNextRetryAt() != null && message.getNextRetryAt().isAfter(now)) {
                // Oldest row still in back-off: later rows wait (order kept).
                return;
            }
            try {
                kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                        .get(properties.publishTimeoutMs(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                markForRetry(message, e, now);
                return;
            }
            log.info("Kafka SEND topic={} key={} type={}", message.getTopic(), message.getMessageKey(),
                    message.getEventType());
            message.setStatus(OutboxMessageStatus.PUBLISHED.name());
            message.setPublishedAt(clock.instant());
            message.setNextRetryAt(null);
            message.setLastError(null);
            repository.save(message);
        }
    }

    private void markForRetry(OutboxMessageInfraEntity message, Exception error, Instant now) {
        int attempts = message.getRetryCount() + 1;
        Duration delay = backOff(attempts);
        message.setRetryCount(attempts);
        message.setNextRetryAt(now.plus(delay));
        message.setLastError(describe(error));
        repository.save(message);
        log.warn("Kafka publish failed for movie event {} ({}), attempt {}, retry in {}s: {}",
                message.getId(), message.getEventType(), attempts, delay.toSeconds(), message.getLastError());
    }

    /** 2s, 4s, 8s ... capped at 60s. */
    static Duration backOff(int attempts) {
        long delay = RETRY_INITIAL_DELAY.toMillis() << Math.min(Math.max(attempts - 1, 0), 20);
        return Duration.ofMillis(Math.min(delay, RETRY_MAX_DELAY.toMillis()));
    }

    private static String describe(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String text = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        return text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) : text;
    }
}
