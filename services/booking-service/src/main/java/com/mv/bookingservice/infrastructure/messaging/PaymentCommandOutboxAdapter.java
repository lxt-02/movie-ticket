package com.mv.bookingservice.infrastructure.messaging;

import com.mv.bookingservice.application.port.out.OutboxPort;
import com.mv.bookingservice.application.port.out.PaymentCommandPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.outbox.OutboxEvent;
import com.mv.bookingservice.infrastructure.config.JsonCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Writes payment commands into outbox_events. Message format (topic payment-commands, key = bookingId):
 * <pre>
 * { commandId, aggregateId, commandType: "CREATE_PAYMENT", occurredAt,
 *   payload: { bookingId, userId, amount, currency, provider, description, expiredAt, idempotencyKey } }
 * { commandId, aggregateId, commandType: "CANCEL_PAYMENT", occurredAt, payload: { bookingId, reason } }
 * { commandId, aggregateId, commandType: "REFUND_PAYMENT", occurredAt,
 *   payload: { bookingId, paymentId, amount, currency, reason } }
 * </pre>
 * The idempotency key carries the attempt number, so "pay again" is a new key and payment-service replaces
 * the previous link, while a redelivered command (same key) returns the same payment.
 */
@Component
@RequiredArgsConstructor
public class PaymentCommandOutboxAdapter implements PaymentCommandPort {

    public static final String CREATE_PAYMENT = "CREATE_PAYMENT";
    public static final String CANCEL_PAYMENT = "CANCEL_PAYMENT";
    public static final String REFUND_PAYMENT = "REFUND_PAYMENT";
    private static final String PROVIDER = "PAYOS";

    private final OutboxPort outboxPort;
    private final JsonCodec json;

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
        write(booking, CREATE_PAYMENT, payload);
    }

    @Override
    public void cancelPayments(Booking booking, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", booking.getId());
        payload.put("reason", reason);
        write(booking, CANCEL_PAYMENT, payload);
    }

    @Override
    public void requestRefund(Booking booking, UUID paymentId, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", booking.getId());
        payload.put("paymentId", paymentId);
        payload.put("amount", booking.getTotalAmount());
        payload.put("currency", booking.getCurrency());
        payload.put("reason", reason);
        write(booking, REFUND_PAYMENT, payload);
    }

    private void write(Booking booking, String commandType, Map<String, Object> payload) {
        UUID commandId = UUID.randomUUID();
        Instant now = Instant.now();

        Map<String, Object> command = new LinkedHashMap<>();
        command.put("commandId", commandId);
        command.put("aggregateId", booking.getId());
        command.put("commandType", commandType);
        command.put("occurredAt", now);
        command.put("payload", payload);

        outboxPort.save(OutboxEvent.builder()
                .id(commandId)
                .aggregateId(booking.getId())
                .eventType(commandType)
                .payload(json.write(command))
                .createdAt(now)
                .build());
    }
}
