package com.mv.bookingservice.infrastructure.messaging;

import com.mv.bookingservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.mv.bookingservice.infrastructure.persistence.entity.OutboxStatus;
import com.mv.bookingservice.infrastructure.persistence.repository.OutboxEventJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Publishes PENDING outbox_events to Kafka, oldest first.
 * <p>
 * <b>Several booking-service instances</b> (load balancing): every instance runs this job, but only one may publish
 * at a time, otherwise the same command is sent twice and CREATE / CANCEL of one booking can overtake each other.
 * Each run opens a transaction and locks the single row of {@code outbox_relay_lock} with
 * {@code FOR UPDATE SKIP LOCKED}; an instance that does not get it skips the run (no waiting, no deadlock). If the
 * active instance dies, its lock is released with its connection and another instance takes over on its next run.
 * <p>
 * <b>Failures</b> (see {@link KafkaPublishErrors}):
 * <ul>
 *     <li>Temporary (broker down, topic not found, timeout): the row stays PENDING with {@code retry_count},
 *     {@code last_error} and a back-off ({@code next_retry_at}: 2s, 4s, 8s ... max {@code relay-retry-max-delay-ms}).
 *     The run stops and the relay pauses until then: every other row would hit the same broker problem.</li>
 *     <li>Permanent (no permission, record too large, unknown event type): the row becomes FAILED and the run
 *     <b>continues with the other bookings</b>. Later rows of the same booking wait behind it (order kept).
 *     Fix the cause, then POST /api/v1/bookings/admin/outbox/requeue-failed.</li>
 * </ul>
 * At-least-once: a crash between the Kafka ack and the commit re-sends the command, which payment-service handles
 * idempotently (same idempotencyKey / commandId).
 */
@Slf4j
@Component
public class BookingOutboxRelay {

    static final String LOCK_NAME = "booking-outbox";
    static final int MAX_ERROR_LENGTH = 2000;

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BookingKafkaProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final Clock clock;

    /** Temporary broker problem: no run before this instant (one probe per back-off, not one per row). */
    private volatile Instant pausedUntil = Instant.EPOCH;

    @Autowired
    public BookingOutboxRelay(OutboxEventJpaRepository repository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              BookingKafkaProperties properties,
                              TransactionTemplate transactionTemplate,
                              EntityManager entityManager) {
        this(repository, kafkaTemplate, properties, transactionTemplate, entityManager, Clock.systemUTC());
    }

    BookingOutboxRelay(OutboxEventJpaRepository repository,
                       KafkaTemplate<String, String> kafkaTemplate,
                       BookingKafkaProperties properties,
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
            fixedDelayString = "${booking.kafka.relay-delay-ms:2000}",
            initialDelayString = "${booking.kafka.relay-initial-delay-ms:3000}"
    )
    public void relay() {
        if (clock.instant().isBefore(pausedUntil)) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            if (!tryLock()) {
                log.debug("Outbox relay is running on another instance, skipping this run");
                return;
            }
            publishBatch();
        });
    }

    /** Until when the relay waits after a temporary broker error (EPOCH = not paused). */
    public Instant pausedUntil() {
        return pausedUntil;
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
    private void publishBatch() {
        Instant now = clock.instant();
        List<OutboxEventEntity> pending = repository.findPublishable(properties.relayBatchSize());
        // Bookings whose older command is not delivered yet: their later commands must wait (order per booking).
        Set<UUID> held = new HashSet<>();
        for (OutboxEventEntity event : pending) {
            UUID bookingId = event.getAggregateId();
            if (held.contains(bookingId)) {
                continue;
            }
            if (event.getNextRetryAt() != null && event.getNextRetryAt().isAfter(now)) {
                held.add(bookingId);
                continue;
            }
            String topic = topicFor(event.getEventType());
            if (topic == null) {
                markFailed(event, "No Kafka topic for outbox event type " + event.getEventType());
                held.add(bookingId);
                continue;
            }
            try {
                kafkaTemplate.send(topic, bookingId.toString(), event.getPayload())
                        .get(properties.publishTimeoutMs(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                Throwable cause = KafkaPublishErrors.rootCause(e);
                if (KafkaPublishErrors.isPermanent(cause)) {
                    markFailed(event, KafkaPublishErrors.describe(cause, MAX_ERROR_LENGTH));
                    held.add(bookingId);
                    continue;
                }
                markForRetry(event, cause, now);
                pausedUntil = event.getNextRetryAt();
                return;
            }
            log.info("Kafka SEND topic={} key={} type={} body={}",
                    topic, bookingId, event.getEventType(), event.getPayload());
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(clock.instant());
            event.setNextRetryAt(null);
            event.setLastError(null);
            repository.save(event);
        }
    }

    private void markForRetry(OutboxEventEntity event, Throwable cause, Instant now) {
        int attempts = event.getRetryCount() + 1;
        Duration delay = backOff(attempts);
        event.setRetryCount(attempts);
        event.setNextRetryAt(now.plus(delay));
        event.setLastError(KafkaPublishErrors.describe(cause, MAX_ERROR_LENGTH));
        repository.save(event);
        if (attempts >= properties.relayAlertAfterAttempts()) {
            log.error("OUTBOX still cannot publish {} ({}) after {} attempts, next try in {}s: {}",
                    event.getId(), event.getEventType(), attempts, delay.toSeconds(), event.getLastError());
        } else {
            log.warn("OUTBOX temporary publish error for {} ({}), attempt {}, relay paused {}s: {}",
                    event.getId(), event.getEventType(), attempts, delay.toSeconds(), event.getLastError());
        }
    }

    private void markFailed(OutboxEventEntity event, String error) {
        event.setStatus(OutboxStatus.FAILED);
        event.setRetryCount(event.getRetryCount() + 1);
        event.setNextRetryAt(null);
        event.setLastError(error);
        repository.save(event);
        log.error("OUTBOX FAILED {} ({}) for booking {}: {}. Other bookings continue; fix the cause, then "
                        + "POST /api/v1/bookings/admin/outbox/requeue-failed",
                event.getId(), event.getEventType(), event.getAggregateId(), error);
    }

    /** 2s, 4s, 8s ... capped. */
    Duration backOff(int attempts) {
        long initial = properties.relayRetryInitialDelayMs();
        long max = properties.relayRetryMaxDelayMs();
        long delay = initial << Math.min(attempts - 1, 20);
        return Duration.ofMillis(Math.min(delay, max));
    }

    private String topicFor(String eventType) {
        if (PaymentCommandOutboxAdapter.CREATE_PAYMENT.equals(eventType)
                || PaymentCommandOutboxAdapter.CANCEL_PAYMENT.equals(eventType)) {
            return properties.paymentCommands();
        }
        return null;
    }
}
