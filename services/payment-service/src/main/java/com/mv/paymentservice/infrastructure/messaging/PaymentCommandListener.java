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
 * Nhận lệnh từ booking-service (topic payment-commands): CREATE_PAYMENT, CANCEL_PAYMENT.
 * <ul>
 *     <li>CREATE_PAYMENT lỗi (dữ liệu sai, PayOS lỗi): trả lời bằng event PAYMENT_CREATE_FAILED (ghi trong
 *     transaction mới) và xác nhận message, <b>không</b> thử lại.</li>
 *     <li>Lỗi database của chính payment-service, hoặc lỗi khi ghi PAYMENT_CREATE_FAILED: ném lại để Kafka thử lại,
 *     hết lượt thì vào payment-commands.DLT.</li>
 *     <li>Lệnh không bao giờ xử lý được (JSON hỏng, commandType lạ, thiếu payload): sang DLT ngay, không thử lại
 *     ({@link NonRetryableMessageException}), giữ lại để điều tra thay vì vứt bỏ.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
// [VI] Consumer topic payment-commands: nhận lệnh từ booking và gọi use case tương ứng.
public class PaymentCommandListener {

    private final JsonCodec json;
    private final CreatePaymentUseCase createPaymentUseCase;
    private final RecordPaymentCreationFailureUseCase recordFailureUseCase;
    private final CancelBookingPaymentsUseCase cancelBookingPaymentsUseCase;

    /** Đọc message rồi chuyển cho hàm xử lý theo commandType. */
    @KafkaListener(
            topics = "${payment.kafka.payment-commands:payment-commands}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}"
    )
    // [VI] Parse JSON -> rẽ nhánh theo commandType. commandType lạ (sai hợp đồng) -> lỗi không retry, sang DLT ngay.
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Kafka RECV topic={} partition={} offset={} key={} body={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());
        PaymentCommandMessage message = parse(record);

        if (PaymentCommandMessage.CANCEL_PAYMENT.equals(message.commandType())) {
            onCancelPayment(message);
        } else if (PaymentCommandMessage.CREATE_PAYMENT.equals(message.commandType())) {
            onCreatePayment(message);
        } else {
            throw new NonRetryableMessageException("Unknown commandType " + message.commandType()
                    + " (commandId " + message.commandId() + ")", null);
        }
    }

    /** JSON → message; JSON hỏng thì ném lỗi "không thử lại" (sang DLT ngay). */
    private PaymentCommandMessage parse(ConsumerRecord<String, String> record) {
        try {
            return json.read(record.value(), PaymentCommandMessage.class);
        } catch (IllegalArgumentException e) {
            // Không vứt bỏ: sang thẳng payment-commands.DLT (không retry) để giữ lại điều tra.
            log.error("Malformed payment command at {}-{}@{}, sending it to the DLT: {}",
                    record.topic(), record.partition(), record.offset(), e.getMessage());
            throw new NonRetryableMessageException("Malformed payment command: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo link thanh toán. Lỗi nghiệp vụ / PayOS → ghi PAYMENT_CREATE_FAILED cho booking biết; lỗi database của mình
     * → ném lại để Kafka thử lại (không báo nhầm cho booking rằng PayOS lỗi).
     */
    // [VI] CREATE_PAYMENT -> tạo payment + link PayOS. Phân loại lỗi:
    //  - DomainException (dữ liệu sai)    -> PAYMENT_CREATE_FAILED / INVALID_PAYMENT_REQUEST (booking bị huỷ)
    //  - lỗi DB / transaction             -> ném lại cho Kafka retry
    //  - lỗi khác (PayOS lỗi, timeout...) -> PAYMENT_CREATE_FAILED / PAYMENT_PROVIDER_ERROR (booking giữ PENDING)
    // Gửi lặp cùng idempotencyKey thì trả lại đúng payment cũ, không tạo link thứ hai.
    private void onCreatePayment(PaymentCommandMessage message) {
        if (message.payload() == null) {
            throw new NonRetryableMessageException("CREATE_PAYMENT without payload (commandId "
                    + message.commandId() + ")", null);
        }
        CreatePaymentCommand command = toCreateCommand(message);
        try {
            createPaymentUseCase.createPayment(command);
        } catch (DomainException e) {
            recordFailureUseCase.recordFailure(command, PaymentEventFactory.REASON_INVALID_REQUEST, e.getMessage());
        } catch (DataAccessException | TransactionException e) {
            log.error("CREATE_PAYMENT for booking {} hit a database error, will be retried: {}",
                    command.getBookingId(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("CREATE_PAYMENT failed for booking {}", command.getBookingId(), e);
            recordFailureUseCase.recordFailure(command, PaymentEventFactory.REASON_PROVIDER_ERROR, e.getMessage());
        }
    }

    /** Huỷ các thanh toán còn mở của booking. Lỗi database ném lại để Kafka giao lại; lỗi PayOS do use case tự xử lý. */
    // [VI] CANCEL_PAYMENT (booking huỷ / hết hạn) -> đóng mọi link còn mở của booking để khách không trả nhầm.
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

    private static CreatePaymentCommand toCreateCommand(PaymentCommandMessage message) {
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
