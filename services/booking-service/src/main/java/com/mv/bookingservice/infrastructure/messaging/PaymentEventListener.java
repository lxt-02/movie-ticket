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
 * Consumes payment-events from payment-service.
 * <ul>
 *     <li>{@link #onMessage}: the main topic. Technical errors are retried with backoff, then the record goes to
 *     {@code payment-events.DLT} (see KafkaConfig). An unreadable record (malformed JSON, no eventId) goes to the
 *     DLT at once, without retries ({@link NonRetryableMessageException}).</li>
 *     <li>{@link #onDeadLetter}: replays the DLT. Not started on boot: started on demand once the cause is fixed
 *     (POST /api/v1/bookings/admin/payment-events-dlt/replay). Safe to replay: the handlers are state-guarded, so
 *     an event already applied changes nothing.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    /** Listener container id of the DLT replay (see PaymentEventsOpsController). */
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
        process(record);
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
            process(record);
        } catch (NonRetryableMessageException e) {
            // Still unreadable: it stays in the DLT (Kafka keeps it), do not send it to the DLT again (loop).
            log.error("DLT REPLAY skipped unreadable record {}-{}@{}: {}",
                    record.topic(), record.partition(), record.offset(), e.getMessage());
        }
    }

    private void process(ConsumerRecord<String, String> record) {
        PaymentEventMessage message;
        try {
            message = json.read(record.value(), PaymentEventMessage.class);
        } catch (IllegalArgumentException e) {
            // Not dropped: straight to payment-events.DLT (no retry), so the event is kept for investigation.
            log.error("Malformed payment event at {}-{}@{}, sending it to the DLT: {}",
                    record.topic(), record.partition(), record.offset(), e.getMessage());
            throw new NonRetryableMessageException("Malformed payment event: " + e.getMessage(), e);
        }
        if (message.eventId() == null || message.eventType() == null) {
            throw new NonRetryableMessageException("Payment event without eventId / eventType", null);
        }
        PaymentEventMessage.Payload p = message.payload();
        String reason = p == null ? null
                : p.reason() != null ? p.reason() + (p.message() != null ? ": " + p.message() : "")
                : p.failureReason();
        handlePaymentEventUseCase.handle(PaymentEventCommand.builder()
                .eventId(message.eventId())
                .eventType(message.eventType())
                .bookingId(p != null && p.bookingId() != null ? p.bookingId() : message.aggregateId())
                .paymentId(p == null ? null : p.paymentId())
                .amount(p == null ? null : p.amount())
                .currency(p == null ? null : p.currency())
                .paidAt(p == null ? null : p.paidAt())
                .reasonCode(p == null ? null : p.reason())
                .reason(reason)
                .build());
    }
}
