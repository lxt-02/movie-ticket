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
 * Outbox relay của movie-service: gửi các dòng PENDING trong {@code outbox_messages} lên topic movie-events
 * (key = movieId), cũ nhất trước, rồi đánh dấu PUBLISHED.
 * <ul>
 *     <li><b>Nhiều instance</b>: mỗi lượt giữ khoá {@code outbox_relay_lock} ({@code FOR UPDATE SKIP LOCKED});
 *     instance không giữ được khoá bỏ lượt, nên event không bị gửi trùng và các event của một phim giữ thứ tự.</li>
 *     <li><b>Gửi lỗi</b>: dòng vẫn PENDING, tăng {@code retry_count}, ghi {@code last_error}, hẹn
 *     {@code next_retry_at} (2s, 4s, 8s ... tối đa 60s). Dừng lượt tại đó: không gửi gì thêm cho tới khi dòng này
 *     được thử lại (giữ thứ tự).</li>
 *     <li><b>At-least-once</b>: sập giữa lúc Kafka ack và lúc commit thì event được gửi lại; search-service xử lý
 *     idempotent (upsert theo movieId, so phiên bản theo updatedAt).</li>
 * </ul>
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

    /** Một lượt chạy: giữ khoá → gửi lô PENDING. */
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

    /** Giữ khoá dòng {@code outbox_relay_lock} tới hết transaction; instance khác đang giữ thì trả false ngay. */
    private boolean tryLock() {
        List<?> locked = entityManager
                .createNativeQuery("SELECT name FROM outbox_relay_lock WHERE name = :name FOR UPDATE SKIP LOCKED")
                .setParameter("name", LOCK_NAME)
                .getResultList();
        return !locked.isEmpty();
    }

    /**
     * Gửi lần lượt; dừng ở dòng đầu tiên chưa gửi được (để giữ thứ tự). Không bao giờ ném lỗi: kết quả từng dòng
     * được lưu khi transaction commit.
     */
    void publishBatch() {
        Instant now = clock.instant();
        List<OutboxMessageInfraEntity> pending = repository.findPending(PageRequest.of(0, properties.relayBatchSize()));
        for (OutboxMessageInfraEntity message : pending) {
            if (isWaitingForRetry(message, now)) {
                return;
            }
            if (!send(message, now)) {
                return;
            }
        }
    }

    /** Dòng đã lỗi trước đó và chưa tới giờ thử lại. */
    private static boolean isWaitingForRetry(OutboxMessageInfraEntity message, Instant now) {
        return message.getNextRetryAt() != null && message.getNextRetryAt().isAfter(now);
    }

    /** Gửi một message, chờ ack, ghi kết quả. Trả {@code false} nếu chưa gửi được (lượt phải dừng). */
    private boolean send(OutboxMessageInfraEntity message, Instant now) {
        try {
            kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                    .get(properties.publishTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            markForRetry(message, e, now);
            return false;
        }
        markPublished(message);
        return true;
    }

    private void markPublished(OutboxMessageInfraEntity message) {
        log.info("Kafka SEND topic={} key={} type={}", message.getTopic(), message.getMessageKey(),
                message.getEventType());
        message.setStatus(OutboxMessageStatus.PUBLISHED.name());
        message.setPublishedAt(clock.instant());
        message.setNextRetryAt(null);
        message.setLastError(null);
        repository.save(message);
    }

    /** Vẫn PENDING, hẹn giờ thử lại theo back-off. */
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

    /** 2s, 4s, 8s ... tối đa 60s. */
    static Duration backOff(int attempts) {
        long delay = RETRY_INITIAL_DELAY.toMillis() << Math.min(Math.max(attempts - 1, 0), 20);
        return Duration.ofMillis(Math.min(delay, RETRY_MAX_DELAY.toMillis()));
    }

    /** "Tên lỗi gốc: message", cắt còn {@link #MAX_ERROR_LENGTH} ký tự. */
    private static String describe(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String text = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        return text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) : text;
    }
}
