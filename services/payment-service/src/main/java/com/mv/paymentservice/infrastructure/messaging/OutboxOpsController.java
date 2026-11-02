package com.mv.paymentservice.infrastructure.messaging;

import com.mv.paymentservice.api.dto.response.ApiResponse;
import com.mv.paymentservice.infrastructure.persistence.entity.OutboxMessageEntity;
import com.mv.paymentservice.infrastructure.persistence.entity.OutboxStatus;
import com.mv.paymentservice.infrastructure.persistence.repository.OutboxMessageJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Outbox operations (ADMIN / STAFF, see SecurityConfig), through the gateway:
 * <ul>
 *     <li>GET  /api/v1/payments/admin/outbox: backlog (PENDING / PROCESSING), FAILED rows with their error</li>
 *     <li>POST /api/v1/payments/admin/outbox/requeue-failed: FAILED -> PENDING once the cause is fixed.
 *     Safe: booking-service skips an event it already applied (inbox by eventId).</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/admin/outbox")
@RequiredArgsConstructor
public class OutboxOpsController {

    private final OutboxMessageJpaRepository repository;
    private final TransactionTemplate transactionTemplate;

    @GetMapping
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.success("Trạng thái outbox", view());
    }

    @PostMapping("/requeue-failed")
    public ApiResponse<Map<String, Object>> requeueFailed() {
        Integer requeued = transactionTemplate.execute(s -> repository.requeueFailed());
        log.warn("OUTBOX requeue: {} FAILED message(s) back to PENDING", requeued);
        Map<String, Object> view = view();
        view.put("requeued", requeued);
        return ApiResponse.success("Đã đưa " + requeued + " message FAILED về PENDING để gửi lại", view);
    }

    private Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("pending", repository.countByStatus(OutboxStatus.PENDING));
        view.put("processing", repository.countByStatus(OutboxStatus.PROCESSING));
        view.put("failed", repository.countByStatus(OutboxStatus.FAILED));
        view.put("oldestPendingAt", repository.oldestCreatedAt(OutboxStatus.PENDING));
        List<Map<String, Object>> failed = repository.findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus.FAILED)
                .stream().map(OutboxOpsController::row).toList();
        view.put("failedRows", failed);
        return view;
    }

    private static Map<String, Object> row(OutboxMessageEntity m) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", m.getId());
        row.put("bookingId", m.getAggregateId());
        row.put("eventType", m.getEventType());
        row.put("topic", m.getTopic());
        row.put("retryCount", m.getRetryCount());
        row.put("lastError", m.getLastError());
        row.put("createdAt", m.getCreatedAt());
        return row;
    }
}
