package com.mv.paymentservice.infrastructure.messaging;

import com.mv.paymentservice.infrastructure.persistence.entity.OutboxMessageEntity;
import com.mv.paymentservice.infrastructure.persistence.entity.OutboxStatus;
import com.mv.paymentservice.infrastructure.persistence.repository.OutboxMessageJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Publishes outbox_messages to Kafka.
 * <ol>
 *     <li>Claim a batch (FOR UPDATE SKIP LOCKED) and mark it PROCESSING with a deadline, then commit.</li>
 *     <li>Send each message and wait for the broker ack.</li>
 *     <li>Mark PUBLISHED; or, on a <b>temporary</b> error (broker down, topic missing, timeout), back to PENDING with
 *     an exponential back-off (retry-delay-ms x 2^n, max max-retry-delay-ms) and the rest of the batch is released
 *     at once (it would hit the same broker problem); or, on a <b>permanent</b> error (no permission, record too
 *     large), FAILED. See {@link KafkaPublishErrors}.</li>
 * </ol>
 * A temporary error never makes a message FAILED: PAYMENT_SUCCEEDED must survive a long broker outage. FAILED rows
 * wait for an operator: POST /api/v1/payments/admin/outbox/requeue-failed.
 * Messages stuck in PROCESSING past the deadline (instance crashed) are claimed again. Delivery is at-least-once;
 * consumers dedupe by eventId.
 */
@Slf4j
@Component
public class OutboxRelay {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final OutboxMessageJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;
    private final OutboxRelayProperties properties;
    private final Clock clock;

    public OutboxRelay(OutboxMessageJpaRepository repository,
                       KafkaTemplate<String, String> kafkaTemplate,
                       TransactionTemplate transactionTemplate,
                       OutboxRelayProperties properties,
                       Clock clock) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = transactionTemplate;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${payment.outbox.relay-delay-ms:10000}",
            initialDelayString = "${payment.outbox.relay-initial-delay-ms:1000}"
    )
    public void relay() {
        List<OutboxMessageEntity> batch = transactionTemplate.execute(status -> claimBatch());
        if (batch == null || batch.isEmpty()) {
            return;
        }
        for (int i = 0; i < batch.size(); i++) {
            Instant retryAt = publish(batch.get(i));
            if (retryAt != null) {
                release(batch.subList(i + 1, batch.size()), retryAt);
                return;
            }
        }
    }

    private List<OutboxMessageEntity> claimBatch() {
        Instant now = clock.instant();
        List<OutboxMessageEntity> claimed = repository.findClaimableForUpdate(now, properties.batchSize());
        Instant deadline = now.plus(Duration.ofMillis(properties.stalePublishingTimeoutMs()));
        for (OutboxMessageEntity message : claimed) {
            message.setStatus(OutboxStatus.PROCESSING);
            message.setRetryCount(message.getRetryCount() + 1);
            message.setNextRetryAt(deadline);
        }
        return repository.saveAll(claimed);
    }

    /** Returns null when done (published or FAILED), or the retry time after a temporary error. */
    private Instant publish(OutboxMessageEntity message) {
        try {
            kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                    .get(properties.publishTimeoutMs(), TimeUnit.MILLISECONDS);
            log.info("Kafka SEND topic={} key={} type={} body={}",
                    message.getTopic(), message.getMessageKey(), message.getEventType(), message.getPayload());
            transactionTemplate.executeWithoutResult(status -> markPublished(message.getId()));
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Instant retryAt = clock.instant();
            transactionTemplate.executeWithoutResult(status -> markForRetry(message.getId(), e, retryAt));
            return retryAt;
        } catch (Exception e) {
            Throwable cause = KafkaPublishErrors.rootCause(e);
            if (KafkaPublishErrors.isPermanent(cause)) {
                transactionTemplate.executeWithoutResult(status -> markFailed(message.getId(), cause));
                return null;
            }
            Instant retryAt = clock.instant().plus(backOff(message.getRetryCount()));
            transactionTemplate.executeWithoutResult(status -> markForRetry(message.getId(), cause, retryAt));
            return retryAt;
        }
    }

    /** retry-delay-ms, x2 per attempt, capped. */
    Duration backOff(int attempts) {
        long delay = properties.retryDelayMs() << Math.min(Math.max(attempts - 1, 0), 20);
        return Duration.ofMillis(Math.min(delay, properties.maxRetryDelayMs()));
    }

    private void markPublished(UUID id) {
        repository.findById(id).ifPresent(message -> {
            message.setStatus(OutboxStatus.PUBLISHED);
            message.setNextRetryAt(null);
            message.setPublishedAt(clock.instant());
            message.setLastError(null);
            repository.save(message);
        });
    }

    private void markForRetry(UUID id, Throwable error, Instant retryAt) {
        repository.findById(id).ifPresent(message -> {
            message.setStatus(OutboxStatus.PENDING);
            message.setNextRetryAt(retryAt);
            message.setLastError(KafkaPublishErrors.describe(error, MAX_ERROR_LENGTH));
            repository.save(message);
            if (message.getRetryCount() >= properties.maxAttempts()) {
                log.error("OUTBOX still cannot publish {} ({}) after {} attempts, next try at {}: {}",
                        message.getId(), message.getEventType(), message.getRetryCount(), retryAt,
                        message.getLastError());
            } else {
                log.warn("OUTBOX temporary publish error for {} ({}), attempt {}, next try at {}: {}",
                        message.getId(), message.getEventType(), message.getRetryCount(), retryAt,
                        message.getLastError());
            }
        });
    }

    private void markFailed(UUID id, Throwable error) {
        repository.findById(id).ifPresent(message -> {
            message.setStatus(OutboxStatus.FAILED);
            message.setNextRetryAt(null);
            message.setLastError(KafkaPublishErrors.describe(error, MAX_ERROR_LENGTH));
            repository.save(message);
            log.error("OUTBOX FAILED {} ({}) for booking {}: {}. Fix the cause, then "
                            + "POST /api/v1/payments/admin/outbox/requeue-failed",
                    message.getId(), message.getEventType(), message.getAggregateId(), message.getLastError());
        });
    }

    /** Not attempted in this run (broker problem): back to PENDING, this claim does not count as an attempt. */
    private void release(List<OutboxMessageEntity> notAttempted, Instant retryAt) {
        if (notAttempted.isEmpty()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> notAttempted.forEach(claimed ->
                repository.findById(claimed.getId()).ifPresent(message -> {
                    message.setStatus(OutboxStatus.PENDING);
                    message.setRetryCount(Math.max(0, message.getRetryCount() - 1));
                    message.setNextRetryAt(retryAt);
                    repository.save(message);
                })));
        log.warn("OUTBOX broker problem: {} other message(s) of this batch released until {}", notAttempted.size(),
                retryAt);
    }
}
