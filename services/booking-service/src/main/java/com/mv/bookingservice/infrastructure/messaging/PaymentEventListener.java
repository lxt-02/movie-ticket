package com.mv.bookingservice.infrastructure.messaging;

import com.mv.bookingservice.application.command.PaymentEventCommand;
import com.mv.bookingservice.application.port.in.HandlePaymentEventUseCase;
import com.mv.bookingservice.infrastructure.config.JsonCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Nhận kết quả thanh toán từ payment-service (topic payment-events).
 * <ul>
 *     <li>{@link #onMessage}: topic chính. Lỗi kỹ thuật được Kafka thử lại có back-off, hết lượt thì record sang
 *     {@code payment-events.DLT} (xem KafkaConfig). Record không đọc được (JSON hỏng, thiếu eventId) sang DLT ngay,
 *     không thử lại ({@link NonRetryableMessageException}).</li>
 *     <li>{@link #onDeadLetter}: phát lại DLT. Không tự chạy khi khởi động; bật bằng
 *     POST /api/v1/bookings/admin/payment-events-dlt/replay sau khi đã sửa nguyên nhân. Phát lại an toàn vì handler
 *     kiểm tra trạng thái booking: event đã áp dụng rồi thì không đổi gì.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    /** Id của listener phát lại DLT (dùng ở PaymentEventsOpsController). */
    public static final String DLT_REPLAY_LISTENER_ID = "payment-events-dlt-replay";

    private final JsonCodec json;
    private final HandlePaymentEventUseCase handlePaymentEventUseCase;
    private final PaymentEventChaos chaos;

    @KafkaListener(
            topics = "${booking.kafka.payment-events:payment-events}",
            groupId = "${spring.kafka.consumer.group-id:booking-service}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Kafka RECV topic={} partition={} offset={} key={} body={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());
        chaos.maybeFail();
        handle(record);
    }

    @KafkaListener(
            id = DLT_REPLAY_LISTENER_ID,
            topics = "${booking.kafka.payment-events:payment-events}.DLT",
            groupId = "booking-service-dlt-replay",
            autoStartup = "false"
    )
    public void onDeadLetter(ConsumerRecord<String, String> record) {
        log.warn("DLT REPLAY topic={} partition={} offset={} key={} body={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());
        try {
            handle(record);
        } catch (NonRetryableMessageException e) {
            // Vẫn không đọc được: để nguyên trong DLT (Kafka còn giữ), không gửi lại vào DLT (tránh vòng lặp).
            log.error("DLT REPLAY skipped unreadable record {}-{}@{}: {}",
                    record.topic(), record.partition(), record.offset(), e.getMessage());
        }
    }

    /** Đọc record → đổi sang command → giao cho use case. */
    private void handle(ConsumerRecord<String, String> record) {
        PaymentEventMessage message = parse(record);
        handlePaymentEventUseCase.handle(toCommand(message));
    }

    /** JSON → message. Không đọc được hoặc thiếu eventId / eventType thì ném lỗi "không thử lại" (sang DLT ngay). */
    private PaymentEventMessage parse(ConsumerRecord<String, String> record) {
        PaymentEventMessage message;
        try {
            message = json.read(record.value(), PaymentEventMessage.class);
        } catch (IllegalArgumentException e) {
            // Không vứt bỏ: sang thẳng payment-events.DLT (không retry) để giữ lại điều tra.
            log.error("Malformed payment event at {}-{}@{}, sending it to the DLT: {}",
                    record.topic(), record.partition(), record.offset(), e.getMessage());
            throw new NonRetryableMessageException("Malformed payment event: " + e.getMessage(), e);
        }
        if (message.eventId() == null || message.eventType() == null) {
            throw new NonRetryableMessageException("Payment event without eventId / eventType", null);
        }
        return message;
    }

    private static PaymentEventCommand toCommand(PaymentEventMessage message) {
        PaymentEventMessage.Payload p = message.payload();
        if (p == null) {
            return PaymentEventCommand.builder()
                    .eventId(message.eventId())
                    .eventType(message.eventType())
                    .bookingId(message.aggregateId())
                    .build();
        }
        return PaymentEventCommand.builder()
                .eventId(message.eventId())
                .eventType(message.eventType())
                .bookingId(p.bookingId() != null ? p.bookingId() : message.aggregateId())
                .paymentId(p.paymentId())
                .amount(p.amount())
                .currency(p.currency())
                .paidAt(p.paidAt())
                .reasonCode(p.reason())
                .reason(reasonText(p))
                .build();
    }

    /** "PROVIDER_ERROR: chi tiết" khi có mã lý do; không thì dùng failureReason. */
    private static String reasonText(PaymentEventMessage.Payload p) {
        if (p.reason() == null) {
            return p.failureReason();
        }
        return p.message() != null ? p.reason() + ": " + p.message() : p.reason();
    }
}
