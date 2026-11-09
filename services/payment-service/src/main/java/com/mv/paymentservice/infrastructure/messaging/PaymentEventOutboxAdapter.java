package com.mv.paymentservice.infrastructure.messaging;

import com.mv.paymentservice.application.event.PaymentEvent;
import com.mv.paymentservice.application.port.out.OutboxMessagePort;
import com.mv.paymentservice.application.port.out.PaymentEventOutboxPort;
import com.mv.paymentservice.domain.model.outbox.OutboxMessage;
import com.mv.paymentservice.infrastructure.config.JsonCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ghi event thanh toán vào bảng outbox_messages, cùng transaction với thay đổi của payment; {@link OutboxRelay} mới
 * là bên gửi lên Kafka. Topic payment-events, key = bookingId để mọi event của một booking vào cùng partition
 * (giữ thứ tự).
 */
@Component
@RequiredArgsConstructor
public class PaymentEventOutboxAdapter implements PaymentEventOutboxPort {

    static final String AGGREGATE_TYPE = "BOOKING";

    private final OutboxMessagePort outboxMessagePort;
    private final KafkaTopicsProperties topics;
    private final JsonCodec json;

    /** Bọc event thành { eventId, aggregateId, eventType, occurredAt, payload } và lưu dòng PENDING. */
    @Override
    public void append(PaymentEvent event) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", event.eventId());
        envelope.put("aggregateId", event.bookingId());
        envelope.put("eventType", event.eventType().name());
        envelope.put("occurredAt", event.occurredAt());
        envelope.put("payload", event.payload());

        outboxMessagePort.save(OutboxMessage.pending(
                AGGREGATE_TYPE,
                event.bookingId(),
                event.eventType().name(),
                topics.paymentEvents(),
                event.bookingId().toString(),
                json.write(envelope)));
    }
}
