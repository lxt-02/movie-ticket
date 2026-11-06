package com.mv.bookingservice.infrastructure.messaging;

import com.mv.bookingservice.api.dto.response.ApiResponse;
import com.mv.bookingservice.domain.model.outbox.OutboxMessageStatus;
import com.mv.bookingservice.infrastructure.persistence.entity.OutboxMessageInfraEntity;
import com.mv.bookingservice.infrastructure.persistence.repository.OutboxMessageJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Outbox operations (ADMIN / STAFF, see SecurityConfig), through the gateway:
 * <ul>
 *     <li>GET  /api/v1/bookings/admin/outbox: backlog (PENDING), FAILED rows with their error, relay pause</li>
 *     <li>POST /api/v1/bookings/admin/outbox/requeue-failed: FAILED -> PENDING once the cause is fixed
 *     (ACL granted, topic created...). Safe: payment-service dedupes by idempotencyKey.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bookings/admin/outbox")
@RequiredArgsConstructor
public class OutboxOpsController {

    private final OutboxMessageJpaRepository repository;
    private final BookingOutboxRelay relay;
    private final TransactionTemplate transactionTemplate;

    @GetMapping
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.success("Trạng thái outbox", view());
    }

    @PostMapping("/requeue-failed")
    public ApiResponse<Map<String, Object>> requeueFailed() {
        Integer requeued = transactionTemplate.execute(s -> repository.requeueFailed());
        log.warn("OUTBOX requeue: {} FAILED row(s) back to PENDING", requeued);
        Map<String, Object> view = view();
        view.put("requeued", requeued);
        return ApiResponse.success("Đã đưa " + requeued + " message FAILED về PENDING để gửi lại", view);
    }

    private Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("pending", repository.countByStatus(OutboxMessageStatus.PENDING.name()));
        view.put("pendingHeldBehindFailed", repository.countHeldBehindFailed());
        view.put("failed", repository.countByStatus(OutboxMessageStatus.FAILED.name()));
        view.put("oldestPendingAt", repository.oldestCreatedAt(OutboxMessageStatus.PENDING.name()));
        repository.findFirstByStatusOrderByCreatedAtAsc(OutboxMessageStatus.PENDING.name())
                .ifPresent(e -> view.put("oldestPendingLastError", e.getLastError()));
        Instant paused = relay.pausedUntil();
        view.put("relayPausedUntil", paused.isAfter(Instant.now()) ? paused : null);
        List<Map<String, Object>> failed = repository.findTop20ByStatusOrderByCreatedAtAsc(OutboxMessageStatus.FAILED.name())
                .stream().map(OutboxOpsController::row).toList();
        view.put("failedRows", failed);
        return view;
    }

    private static Map<String, Object> row(OutboxMessageInfraEntity e) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", e.getId());
        row.put("bookingId", e.getAggregateId());
        row.put("eventType", e.getEventType());
        row.put("topic", e.getTopic());
        row.put("retryCount", e.getRetryCount());
        row.put("lastError", e.getLastError());
        row.put("createdAt", e.getCreatedAt());
        return row;
    }
}
