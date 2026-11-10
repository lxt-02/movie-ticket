package com.mv.bookingservice.infrastructure.messaging;

import com.mv.bookingservice.domain.model.outbox.OutboxMessageStatus;
import com.mv.bookingservice.infrastructure.persistence.entity.OutboxMessageInfraEntity;
import com.mv.bookingservice.infrastructure.persistence.repository.OutboxMessageJpaRepository;
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
 * Outbox relay của booking-service: đọc các dòng PENDING trong {@code outbox_messages} (cũ nhất trước) và gửi
 * lên Kafka (topic payment-commands, key = bookingId).
 *
 * <h3>Một lượt chạy (mỗi {@code relay-delay-ms})</h3>
 * <ol>
 *     <li>Đang tạm dừng vì broker lỗi ({@link #pausedUntil()}) thì bỏ lượt.</li>
 *     <li>Mở transaction, giữ khoá {@code outbox_relay_lock} ({@code FOR UPDATE SKIP LOCKED}). Chạy nhiều instance
 *     booking thì chỉ instance giữ được khoá mới gửi, nên không gửi trùng và CREATE / CANCEL của một booking không
 *     vượt mặt nhau. Instance giữ khoá chết thì khoá tự nhả theo connection.</li>
 *     <li>Gửi từng message và chờ broker xác nhận (ack). Kết quả của mỗi dòng được lưu khi transaction commit.</li>
 * </ol>
 *
 * <h3>Khi gửi lỗi (phân loại ở {@link KafkaPublishErrors})</h3>
 * <ul>
 *     <li><b>Lỗi tạm thời</b> (broker sập, chưa có topic, timeout): dòng vẫn PENDING, tăng {@code retry_count}, ghi
 *     {@code last_error}, hẹn {@code next_retry_at} (2s, 4s, 8s ... tối đa {@code relay-retry-max-delay-ms}).
 *     Dừng cả lượt và tạm dừng relay tới lúc đó, vì các dòng khác cũng sẽ gặp đúng lỗi broker này.</li>
 *     <li><b>Lỗi vĩnh viễn</b> (không có quyền, message quá lớn): dòng thành FAILED, các booking khác <b>vẫn gửi
 *     tiếp</b>; các message sau của cùng booking phải chờ (giữ thứ tự). Sửa nguyên nhân rồi gọi
 *     POST /api/v1/bookings/admin/outbox/requeue-failed.</li>
 * </ul>
 *
 * At-least-once: sập giữa lúc Kafka đã ack và lúc commit thì lượt sau gửi lại; payment-service xử lý idempotent
 * (cùng idempotencyKey / commandId) nên không tạo hai thanh toán.
 */
@Slf4j
@Component
public class BookingOutboxRelay {

    static final String LOCK_NAME = "booking-outbox";
    static final int MAX_ERROR_LENGTH = 2000;

    /** Kết quả gửi một message, quyết định lượt chạy làm gì tiếp. */
    private enum SendOutcome {
        /** Đã gửi xong: sang message tiếp theo. */
        SENT,
        /** Lỗi vĩnh viễn, dòng đã FAILED: bỏ qua các message còn lại của booking này. */
        FAILED_FOR_GOOD,
        /** Lỗi tạm thời hoặc thread bị ngắt: dừng cả lượt. */
        STOP_RUN
    }

    private final OutboxMessageJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BookingKafkaProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final Clock clock;

    /** Broker đang lỗi tạm thời: không chạy lượt nào trước thời điểm này (thử lại một lần mỗi back-off). */
    private volatile Instant pausedUntil = Instant.EPOCH;

    @Autowired
    public BookingOutboxRelay(OutboxMessageJpaRepository repository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              BookingKafkaProperties properties,
                              TransactionTemplate transactionTemplate,
                              EntityManager entityManager) {
        this(repository, kafkaTemplate, properties, transactionTemplate, entityManager, Clock.systemUTC());
    }

    BookingOutboxRelay(OutboxMessageJpaRepository repository,
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

    /** Một lượt chạy của relay (xem các bước ở mô tả class). */
    @Scheduled(
            fixedDelayString = "${booking.kafka.relay-delay-ms:2000}",
            initialDelayString = "${booking.kafka.relay-initial-delay-ms:3000}"
    )
    public void relay() {
        if (isPaused()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            if (!tryLock()) {
                log.debug("Outbox relay is running on another instance, skipping this run");
                return;
            }
            publishPendingMessages();
        });
    }

    /** Relay tạm dừng tới khi nào sau lỗi broker tạm thời (EPOCH = không tạm dừng). */
    public Instant pausedUntil() {
        return pausedUntil;
    }

    private boolean isPaused() {
        return clock.instant().isBefore(pausedUntil);
    }

    /** Giữ khoá dòng {@code outbox_relay_lock} tới hết transaction; instance khác đang giữ thì trả false ngay. */
    private boolean tryLock() {
        List<?> locked = entityManager
                .createNativeQuery("SELECT name FROM outbox_relay_lock WHERE name = :name FOR UPDATE SKIP LOCKED")
                .setParameter("name", LOCK_NAME)
                .getResultList();
        return !locked.isEmpty();
    }

    /**
     * Gửi một lô message PENDING theo thứ tự cũ → mới. Không bao giờ ném lỗi: kết quả từng dòng nằm trong entity,
     * được lưu khi transaction commit.
     */
    private void publishPendingMessages() {
        Instant now = clock.instant();
        // Booking có message cũ hơn chưa gửi được: các message sau của booking đó phải chờ (giữ thứ tự theo booking).
        Set<UUID> blockedBookings = new HashSet<>();

        for (OutboxMessageInfraEntity message : repository.findPublishable(properties.relayBatchSize())) {
            UUID bookingId = message.getAggregateId();
            if (blockedBookings.contains(bookingId)) {
                continue;
            }
            if (isWaitingForRetry(message, now)) {
                blockedBookings.add(bookingId);
                continue;
            }

            SendOutcome outcome = send(message, now);
            if (outcome == SendOutcome.STOP_RUN) {
                return;
            }
            if (outcome == SendOutcome.FAILED_FOR_GOOD) {
                blockedBookings.add(bookingId);
            }
        }
    }

    /** Dòng đã lỗi trước đó và chưa tới giờ thử lại. */
    private static boolean isWaitingForRetry(OutboxMessageInfraEntity message, Instant now) {
        return message.getNextRetryAt() != null && message.getNextRetryAt().isAfter(now);
    }

    /** Gửi một message lên Kafka, chờ ack, rồi ghi kết quả vào dòng outbox. */
    private SendOutcome send(OutboxMessageInfraEntity message, Instant now) {
        try {
            kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                    .get(properties.publishTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SendOutcome.STOP_RUN;
        } catch (Exception e) {
            Throwable cause = KafkaPublishErrors.rootCause(e);
            if (KafkaPublishErrors.isPermanent(cause)) {
                markFailed(message, KafkaPublishErrors.describe(cause, MAX_ERROR_LENGTH));
                return SendOutcome.FAILED_FOR_GOOD;
            }
            markForRetry(message, cause, now);
            pausedUntil = message.getNextRetryAt();
            return SendOutcome.STOP_RUN;
        }
        markPublished(message);
        return SendOutcome.SENT;
    }

    private void markPublished(OutboxMessageInfraEntity message) {
        log.info("Kafka SEND topic={} key={} type={} body={}",
                message.getTopic(), message.getMessageKey(), message.getEventType(), message.getPayload());
        message.setStatus(OutboxMessageStatus.PUBLISHED.name());
        message.setPublishedAt(clock.instant());
        message.setNextRetryAt(null);
        message.setLastError(null);
        repository.save(message);
    }

    /** Lỗi tạm thời: vẫn PENDING, hẹn giờ thử lại theo back-off. */
    private void markForRetry(OutboxMessageInfraEntity message, Throwable cause, Instant now) {
        int attempts = message.getRetryCount() + 1;
        Duration delay = backOff(attempts);
        message.setRetryCount(attempts);
        message.setNextRetryAt(now.plus(delay));
        message.setLastError(KafkaPublishErrors.describe(cause, MAX_ERROR_LENGTH));
        repository.save(message);
        if (attempts >= properties.relayAlertAfterAttempts()) {
            log.error("OUTBOX still cannot publish {} ({}) after {} attempts, next try in {}s: {}",
                    message.getId(), message.getEventType(), attempts, delay.toSeconds(), message.getLastError());
        } else {
            log.warn("OUTBOX temporary publish error for {} ({}), attempt {}, relay paused {}s: {}",
                    message.getId(), message.getEventType(), attempts, delay.toSeconds(), message.getLastError());
        }
    }

    /** Lỗi vĩnh viễn: FAILED, chờ người vận hành sửa rồi requeue. */
    private void markFailed(OutboxMessageInfraEntity message, String error) {
        message.setStatus(OutboxMessageStatus.FAILED.name());
        message.setRetryCount(message.getRetryCount() + 1);
        message.setNextRetryAt(null);
        message.setLastError(error);
        repository.save(message);
        log.error("OUTBOX FAILED {} ({}) for booking {}: {}. Other bookings continue; fix the cause, then "
                        + "POST /api/v1/bookings/admin/outbox/requeue-failed",
                message.getId(), message.getEventType(), message.getAggregateId(), error);
    }

    /** Thời gian chờ trước lần thử thứ {@code attempts}: 2s, 4s, 8s ... không quá relay-retry-max-delay-ms. */
    Duration backOff(int attempts) {
        long initial = properties.relayRetryInitialDelayMs();
        long max = properties.relayRetryMaxDelayMs();
        long delay = initial << Math.min(attempts - 1, 20);
        return Duration.ofMillis(Math.min(delay, max));
    }
}
