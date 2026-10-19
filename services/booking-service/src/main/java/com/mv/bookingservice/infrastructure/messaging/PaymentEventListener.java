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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final JsonCodec json;
    private final HandlePaymentEventUseCase handlePaymentEventUseCase;

    @KafkaListener(
            topics = "${booking.kafka.payment-events:payment-events}",
            groupId = "${spring.kafka.consumer.group-id:booking-service}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Kafka RECV topic={} partition={} offset={} key={} body={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());
        PaymentEventMessage message;
        try {
            message = json.read(record.value(), PaymentEventMessage.class);
        } catch (IllegalArgumentException e) {
            log.error("Dropping malformed payment event at {}-{}@{}: {}",
                    record.topic(), record.partition(), record.offset(), e.getMessage());
            return;
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
