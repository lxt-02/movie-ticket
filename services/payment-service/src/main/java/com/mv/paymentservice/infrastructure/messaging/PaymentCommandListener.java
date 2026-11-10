package com.mv.paymentservice.infrastructure.messaging;

import com.mv.paymentservice.application.command.CreatePaymentCommand;
import com.mv.paymentservice.application.event.PaymentEventFactory;
import com.mv.paymentservice.application.port.in.CancelBookingPaymentsUseCase;
import com.mv.paymentservice.application.port.in.CreatePaymentUseCase;
import com.mv.paymentservice.application.port.in.RecordPaymentCreationFailureUseCase;
import com.mv.paymentservice.domain.model.payment.exception.DomainException;
import com.mv.paymentservice.infrastructure.config.JsonCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

import java.util.UUID;

/**
 * Consumes payment-commands (CREATE_PAYMENT, CANCEL_PAYMENT). A failed CREATE_PAYMENT is answered with PAYMENT_CREATE_FAILED (written in a new
 * transaction) and the message is acknowledged, not retried. Database errors, and an error while recording that failure
 * (e.g. database down), are rethrown so Kafka retries the command, then parks it in payment-commands.DLT.
 * A command that can never be processed (malformed JSON, unknown type, missing payload) goes to the DLT at once,
 * without retries ({@link NonRetryableMessageException}): kept for investigation instead of being dropped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCommandListener {

    private final JsonCodec json;
    private final CreatePaymentUseCase createPaymentUseCase;
    private final RecordPaymentCreationFailureUseCase recordFailureUseCase;
    private final CancelBookingPaymentsUseCase cancelBookingPaymentsUseCase;

    @KafkaListener(
            topics = "${payment.kafka.payment-commands:payment-commands}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Kafka RECV topic={} partition={} offset={} key={} body={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());
        PaymentCommandMessage message;
        try {
            message = json.read(record.value(), PaymentCommandMessage.class);
        } catch (IllegalArgumentException e) {
            // Not dropped: straight to payment-commands.DLT (no retry), kept for investigation.
            log.error("Malformed payment command at {}-{}@{}, sending it to the DLT: {}",
                    record.topic(), record.partition(), record.offset(), e.getMessage());
            throw new NonRetryableMessageException("Malformed payment command: " + e.getMessage(), e);
        }

        if (PaymentCommandMessage.CANCEL_PAYMENT.equals(message.commandType())) {
            onCancelPayment(message);
            return;
        }
        if (!PaymentCommandMessage.CREATE_PAYMENT.equals(message.commandType())) {
            throw new NonRetryableMessageException("Unknown commandType " + message.commandType()
                    + " (commandId " + message.commandId() + ")", null);
        }
        if (message.payload() == null) {
            throw new NonRetryableMessageException("CREATE_PAYMENT without payload (commandId "
                    + message.commandId() + ")", null);
        }

        CreatePaymentCommand command = toCommand(message);
        try {
            createPaymentUseCase.createPayment(command);
        } catch (DomainException e) {
            recordFailureUseCase.recordFailure(command, PaymentEventFactory.REASON_INVALID_REQUEST, e.getMessage());
        } catch (DataAccessException | TransactionException e) {
            // Our own database, not PayOS: retry the command (then DLT) instead of telling booking that PayOS failed.
            log.error("CREATE_PAYMENT for booking {} hit a database error, will be retried: {}",
                    command.getBookingId(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("CREATE_PAYMENT failed for booking {}", command.getBookingId(), e);
            recordFailureUseCase.recordFailure(command, PaymentEventFactory.REASON_PROVIDER_ERROR, e.getMessage());
        }
    }

    /** Database errors propagate so Kafka redelivers; PayOS errors are handled inside the use case. */
    private void onCancelPayment(PaymentCommandMessage message) {
        PaymentCommandMessage.Payload p = message.payload();
        UUID bookingId = p != null && p.bookingId() != null ? p.bookingId() : message.aggregateId();
        if (bookingId == null) {
            throw new NonRetryableMessageException("CANCEL_PAYMENT without bookingId (commandId "
                    + message.commandId() + ")", null);
        }
        int cancelled = cancelBookingPaymentsUseCase.cancelOpenPayments(bookingId, p == null ? null : p.reason());
        log.info("CANCEL_PAYMENT for booking {}: {} payment(s) cancelled", bookingId, cancelled);
    }

    private CreatePaymentCommand toCommand(PaymentCommandMessage message) {
        PaymentCommandMessage.Payload p = message.payload();
        return CreatePaymentCommand.builder()
                .commandId(message.commandId())
                .bookingId(p.bookingId() != null ? p.bookingId() : message.aggregateId())
                .userId(p.userId())
                .amount(p.amount())
                .currency(p.currency())
                .provider(p.provider())
                .description(p.description())
                .expiredAt(p.expiredAt())
                .idempotencyKey(p.idempotencyKey())
                .build();
    }
}
