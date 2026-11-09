package com.mv.paymentservice.infrastructure.messaging;

import com.mv.paymentservice.domain.model.outbox.OutboxMessageStatus;
import com.mv.paymentservice.infrastructure.persistence.entity.OutboxMessageInfraEntity;
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
 * Outbox relay của payment-service: gửi các dòng {@code outbox_messages} lên Kafka (topic payment-events,
 * key = bookingId).
 *
 * <h3>Một lượt chạy</h3>
 * <ol>
 *     <li><b>Nhận lô</b> ({@link #claimBatch()}): khoá các dòng gửi được ({@code FOR UPDATE SKIP LOCKED}), chuyển
 *     PROCESSING kèm hạn chót, commit. Nhiều instance chạy cùng lúc sẽ nhận các lô khác nhau.</li>
 *     <li><b>Gửi từng message</b> ({@link #publish}) và chờ broker ack, mỗi kết quả ghi trong transaction riêng.</li>
 *     <li>Kết quả:
 *         <ul>
 *             <li>Thành công → PUBLISHED.</li>
 *             <li><b>Lỗi tạm thời</b> (broker sập, chưa có topic, timeout) → về PENDING, hẹn thử lại theo back-off
 *             (retry-delay-ms × 2^n, tối đa max-retry-delay-ms). Phần còn lại của lô được nhả ngay
 *             ({@link #release}), vì sẽ gặp cùng lỗi broker.</li>
 *             <li><b>Lỗi vĩnh viễn</b> (không có quyền, message quá lớn) → FAILED.</li>
 *         </ul>
 *     </li>
 * </ol>
 * Lỗi tạm thời không bao giờ làm message FAILED: PAYMENT_SUCCEEDED phải sống qua cả một đợt broker sập dài.
 * Dòng FAILED chờ người vận hành: POST /api/v1/payments/admin/outbox/requeue-failed. Dòng kẹt PROCESSING quá hạn
 * (instance chết giữa chừng) được nhận lại ở lượt sau. Giao ít nhất một lần (at-least-once); booking-service áp
 * dụng event idempotent theo trạng thái booking.
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

    /** Một lượt chạy: nhận lô → gửi lần lượt → gặp lỗi tạm thời thì nhả phần còn lại và dừng. */
    @Scheduled(
            fixedDelayString = "${payment.outbox.relay-delay-ms:10000}",
            initialDelayString = "${payment.outbox.relay-initial-delay-ms:1000}"
    )
    public void relay() {
        List<OutboxMessageInfraEntity> batch = transactionTemplate.execute(status -> claimBatch());
        if (batch == null || batch.isEmpty()) {
            return;
        }
        for (int i = 0; i < batch.size(); i++) {
            Instant retryAt = publish(batch.get(i));
            boolean brokerProblem = retryAt != null;
            if (brokerProblem) {
                release(batch.subList(i + 1, batch.size()), retryAt);
                return;
            }
        }
    }

    /** Khoá và nhận một lô: PROCESSING, tăng số lần thử, hạn chót = now + stale-publishing-timeout. */
    private List<OutboxMessageInfraEntity> claimBatch() {
        Instant now = clock.instant();
        List<OutboxMessageInfraEntity> claimed = repository.findClaimableForUpdate(now, properties.batchSize());
        Instant deadline = now.plus(Duration.ofMillis(properties.stalePublishingTimeoutMs()));
        for (OutboxMessageInfraEntity message : claimed) {
            message.setStatus(OutboxMessageStatus.PROCESSING.name());
            message.setRetryCount(message.getRetryCount() + 1);
            message.setNextRetryAt(deadline);
        }
        return repository.saveAll(claimed);
    }

    /**
     * Gửi một message và chờ ack.
     *
     * @return {@code null} khi đã xong (PUBLISHED hoặc FAILED); thời điểm thử lại khi gặp lỗi tạm thời
     */
    private Instant publish(OutboxMessageInfraEntity message) {
        try {
            kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                    .get(properties.publishTimeoutMs(), TimeUnit.MILLISECONDS);
            log.info("Kafka SEND topic={} key={} type={} body={}",
                    message.getTopic(), message.getMessageKey(), message.getEventType(), message.getPayload());
            inTransaction(() -> markPublished(message.getId()));
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Instant retryAt = clock.instant();
            inTransaction(() -> markForRetry(message.getId(), e, retryAt));
            return retryAt;
        } catch (Exception e) {
            Throwable cause = KafkaPublishErrors.rootCause(e);
            if (KafkaPublishErrors.isPermanent(cause)) {
                inTransaction(() -> markFailed(message.getId(), cause));
                return null;
            }
            Instant retryAt = clock.instant().plus(backOff(message.getRetryCount()));
            inTransaction(() -> markForRetry(message.getId(), cause, retryAt));
            return retryAt;
        }
    }

    /** Thời gian chờ trước lần thử thứ {@code attempts}: retry-delay-ms, gấp đôi mỗi lần, không quá max-retry-delay-ms. */
    Duration backOff(int attempts) {
        long delay = properties.retryDelayMs() << Math.min(Math.max(attempts - 1, 0), 20);
        return Duration.ofMillis(Math.min(delay, properties.maxRetryDelayMs()));
    }

    private void markPublished(UUID id) {
        repository.findById(id).ifPresent(message -> {
            message.setStatus(OutboxMessageStatus.PUBLISHED.name());
            message.setNextRetryAt(null);
            message.setPublishedAt(clock.instant());
            message.setLastError(null);
            repository.save(message);
        });
    }

    /** Lỗi tạm thời: về PENDING, thử lại lúc {@code retryAt}. */
    private void markForRetry(UUID id, Throwable error, Instant retryAt) {
        repository.findById(id).ifPresent(message -> {
            message.setStatus(OutboxMessageStatus.PENDING.name());
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

    /** Lỗi vĩnh viễn: FAILED, chờ người vận hành sửa rồi requeue. */
    private void markFailed(UUID id, Throwable error) {
        repository.findById(id).ifPresent(message -> {
            message.setStatus(OutboxMessageStatus.FAILED.name());
            message.setNextRetryAt(null);
            message.setLastError(KafkaPublishErrors.describe(error, MAX_ERROR_LENGTH));
            repository.save(message);
            log.error("OUTBOX FAILED {} ({}) for booking {}: {}. Fix the cause, then "
                            + "POST /api/v1/payments/admin/outbox/requeue-failed",
                    message.getId(), message.getEventType(), message.getAggregateId(), message.getLastError());
        });
    }

    /**
     * Các message chưa kịp gửi trong lượt này (broker đang lỗi): về PENDING, thử lại lúc {@code retryAt}. Lần nhận
     * lô vừa rồi không tính là một lần thử nên trừ lại {@code retry_count}.
     */
    private void release(List<OutboxMessageInfraEntity> notAttempted, Instant retryAt) {
        if (notAttempted.isEmpty()) {
            return;
        }
        inTransaction(() -> notAttempted.forEach(claimed ->
                repository.findById(claimed.getId()).ifPresent(message -> {
                    message.setStatus(OutboxMessageStatus.PENDING.name());
                    message.setRetryCount(Math.max(0, message.getRetryCount() - 1));
                    message.setNextRetryAt(retryAt);
                    repository.save(message);
                })));
        log.warn("OUTBOX broker problem: {} other message(s) of this batch released until {}", notAttempted.size(),
                retryAt);
    }

    private void inTransaction(Runnable work) {
        transactionTemplate.executeWithoutResult(status -> work.run());
    }
}
