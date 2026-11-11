package com.mv.bookingservice.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Envelope on topic payment-events: { eventId, aggregateId (bookingId), eventType, occurredAt, payload }.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
// [VI] Khuôn JSON booking ĐỌC từ topic payment-events (envelope + payload), map từ các file mẫu
// payment-link-created.json / payment-succeeded.json / payment-create-failed.json.
// ignoreUnknown = true: payment gửi thêm field (checkoutUrl, qrCode, userId...) thì booking bỏ qua, không lỗi.
// Lưu ý: nếu payment ĐỔI TÊN field thì ở đây chỉ nhận null -> chính contract test mới bắt được lỗi này.
public record PaymentEventMessage(
        UUID eventId,
        UUID aggregateId,
        String eventType,
        Instant occurredAt,
        Payload payload
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payload(
            UUID paymentId,
            UUID bookingId,
            String paymentStatus,
            BigDecimal amount,
            String currency,
            String reason,
            String message,
            String failureReason,
            Instant paidAt
    ) {
    }
}
