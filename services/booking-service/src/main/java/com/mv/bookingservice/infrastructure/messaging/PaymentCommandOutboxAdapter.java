package com.mv.bookingservice.infrastructure.messaging;

import com.mv.bookingservice.application.port.out.OutboxMessagePort;
import com.mv.bookingservice.application.port.out.PaymentCommandPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.outbox.OutboxMessage;
import com.mv.bookingservice.infrastructure.config.JsonCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ghi lệnh thanh toán vào bảng outbox_messages, cùng transaction với thay đổi booking; {@link BookingOutboxRelay}
 * mới là bên gửi lên Kafka. Nhờ vậy booking đã lưu thì lệnh chắc chắn được gửi, booking rollback thì lệnh cũng mất.
 * <p>
 * Định dạng message (topic payment-commands, key = bookingId để mọi lệnh của một booking vào cùng partition):
 * <pre>
 * { commandId, aggregateId, commandType: "CREATE_PAYMENT", occurredAt,
 *   payload: { bookingId, userId, amount, currency, provider, description, expiredAt, idempotencyKey } }
 * { commandId, aggregateId, commandType: "CANCEL_PAYMENT", occurredAt, payload: { bookingId, reason } }
 * </pre>
 * idempotencyKey chứa số lần thanh toán: "thanh toán lại" là key mới nên payment-service tạo link mới, còn một
 * lệnh bị giao lại (cùng key) thì trả lại đúng thanh toán cũ.
 */
@Component
@RequiredArgsConstructor
public class PaymentCommandOutboxAdapter implements PaymentCommandPort {

    public static final String CREATE_PAYMENT = "CREATE_PAYMENT";
    public static final String CANCEL_PAYMENT = "CANCEL_PAYMENT";
    public static final String AGGREGATE_TYPE = "BOOKING";
    private static final String PROVIDER = "PAYOS";

    private final OutboxMessagePort outboxMessagePort;
    private final BookingKafkaProperties properties;
    private final JsonCodec json;

    /** CREATE_PAYMENT: yêu cầu payment-service tạo link PayOS cho booking. */
    @Override
    public void requestPayment(Booking booking) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", booking.getId());
        payload.put("userId", booking.getUserId());
        payload.put("amount", booking.getTotalAmount());
        payload.put("currency", booking.getCurrency());
        payload.put("provider", PROVIDER);
        payload.put("description", "Booking " + booking.getBookingCode());
        // The payment link must not outlive the seat hold.
        payload.put("expiredAt", booking.getExpiresAt());
        payload.put("idempotencyKey",
                "payment:create:" + PROVIDER + ":" + booking.getId() + ":" + booking.currentPaymentAttempt());
        saveToOutbox(booking, CREATE_PAYMENT, payload);
    }

    /** CANCEL_PAYMENT: yêu cầu payment-service huỷ các link còn mở của booking. */
    @Override
    public void cancelPayments(Booking booking, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", booking.getId());
        payload.put("reason", reason);
        saveToOutbox(booking, CANCEL_PAYMENT, payload);
    }

    /** Bọc payload thành message { commandId, aggregateId, commandType, occurredAt, payload } và lưu dòng PENDING. */
    private void saveToOutbox(Booking booking, String commandType, Map<String, Object> payload) {
        UUID commandId = UUID.randomUUID();
        Instant now = Instant.now();

        Map<String, Object> command = new LinkedHashMap<>();
        command.put("commandId", commandId);
        command.put("aggregateId", booking.getId());
        command.put("commandType", commandType);
        command.put("occurredAt", now);
        command.put("payload", payload);

        outboxMessagePort.save(OutboxMessage.pending(
                AGGREGATE_TYPE,
                booking.getId(),
                commandType,
                properties.paymentCommands(),
                booking.getId().toString(),
                json.write(command)));
    }
}
