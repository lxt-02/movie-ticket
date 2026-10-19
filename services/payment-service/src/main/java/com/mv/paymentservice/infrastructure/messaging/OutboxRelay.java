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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes outbox_messages to Kafka.
 * <ol>
 *     <li>Claim a batch (FOR UPDATE SKIP LOCKED) and mark it PROCESSING with a deadline, then commit.</li>
 *     <li>Send each message and wait for the broker ack.</li>
 *     <li>Mark PUBLISHED, or back to PENDING with a retry delay, or FAILED after max attempts.</li>
 * </ol>
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
        for (OutboxMessageEntity message : batch) {
            publish(message);
        }
    }

    private List<OutboxMessageEntity> claimBatch() {
        Instant now = clock.instant();
        List<OutboxMessageEntity> claimed = repository.findClaimableForUpdate(
                properties.maxAttempts(), now, properties.batchSize());
        Instant deadline = now.plus(Duration.ofMillis(properties.stalePublishingTimeoutMs()));
        for (OutboxMessageEntity message : claimed) {
            message.setStatus(OutboxStatus.PROCESSING);
            message.setRetryCount(message.getRetryCount() + 1);
            message.setNextRetryAt(deadline);
            message.setLastError(null);
        }
        return repository.saveAll(claimed);
    }

    private void publish(OutboxMessageEntity message) {
        try {
            kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                    .get(properties.publishTimeoutMs(), TimeUnit.MILLISECONDS);
            log.info("Kafka SEND topic={} key={} type={} body={}",
                    message.getTopic(), message.getMessageKey(), message.getEventType(), message.getPayload());
            transactionTemplate.executeWithoutResult(status -> markPublished(message.getId()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            transactionTemplate.executeWithoutResult(status -> markFailedAttempt(message.getId(), e));
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            Throwable cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
            log.warn("Outbox publish failed for {} ({}): {}", message.getId(), message.getEventType(), cause.getMessage());
            transactionTemplate.executeWithoutResult(status -> markFailedAttempt(message.getId(), cause));
        }
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

    private void markFailedAttempt(UUID id, Throwable error) {
        repository.findById(id).ifPresent(message -> {
            boolean exhausted = message.getRetryCount() >= properties.maxAttempts();
            message.setStatus(exhausted ? OutboxStatus.FAILED : OutboxStatus.PENDING);
            message.setNextRetryAt(exhausted ? null
                    : clock.instant().plus(Duration.ofMillis(properties.retryDelayMs())));
            message.setLastError(truncate(error.getClass().getSimpleName() + ": " + error.getMessage()));
            repository.save(message);
            if (exhausted) {
                log.error("Outbox message {} ({}) FAILED after {} attempts",
                        message.getId(), message.getEventType(), message.getRetryCount());
            }
        });
    }

    private static String truncate(String value) {
        return value.length() > MAX_ERROR_LENGTH ? value.substring(0, MAX_ERROR_LENGTH) : value;
    }
}
