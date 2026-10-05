package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.command.CreatePaymentCommand;
import com.mv.paymentservice.application.dto.BookingPaymentContextDto;
import com.mv.paymentservice.application.port.in.CreatePaymentUseCase;
import com.mv.paymentservice.application.port.out.BookingPaymentContextPort;
import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.application.port.out.PaymentGatewayPort;
import com.mv.paymentservice.application.port.out.SaveOutboxEventPort;
import com.mv.paymentservice.application.port.out.SavePaymentPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.exception.IdempotencyConflictException;
import com.mv.paymentservice.domain.model.payment.exception.InvalidPaymentException;
import com.mv.paymentservice.domain.model.payment.exception.PaymentExpiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatePaymentService implements CreatePaymentUseCase {

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final BookingPaymentContextPort bookingPaymentContextPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final SaveOutboxEventPort saveOutboxEventPort;

    @Override
    @Transactional
    public Payment createPayment(CreatePaymentCommand command) {
        // 1. Idempotency Check
        if (command.getIdempotencyKey() != null && !command.getIdempotencyKey().isBlank()) {
            Optional<Payment> existingPaymentOpt = loadPaymentPort.findByUserIdAndIdempotencyKey(
                    command.getUserId(),
                    command.getIdempotencyKey()
            );

            if (existingPaymentOpt.isPresent()) {
                Payment existingPayment = existingPaymentOpt.get();
                if (command.getRequestHash() != null && command.getRequestHash().equals(existingPayment.getRequestHash())) {
                    log.info("Returning idempotent payment {}", existingPayment.getId());
                    return existingPayment;
                } else {
                    throw new IdempotencyConflictException("Idempotency key reused with different request payload");
                }
            }
        }

        // 2. Validate Booking with Booking Service via Port
        BookingPaymentContextDto bookingContext = bookingPaymentContextPort.getPaymentContext(
                        command.getBookingId(),
                        command.getUserId()
                )
                .orElseThrow(() -> new InvalidPaymentException("Booking not found: " + command.getBookingId()));

        if (!bookingContext.getUserId().equals(command.getUserId())) {
            throw new InvalidPaymentException("User is not authorized for booking " + command.getBookingId());
        }

        if (!"PENDING".equalsIgnoreCase(bookingContext.getStatus())) {
            throw new InvalidPaymentException("Booking is not in PENDING status. Current status: " + bookingContext.getStatus());
        }

        if (bookingContext.getExpiresAt() != null && Instant.now().isAfter(bookingContext.getExpiresAt())) {
            throw new PaymentExpiredException(command.getBookingId());
        }

        // 3. Create Payment Aggregate
        UUID paymentId = UUID.randomUUID();
        String paymentUrl = paymentGatewayPort.generatePaymentUrl(
                Payment.builder()
                        .id(paymentId)
                        .bookingId(command.getBookingId())
                        .amount(bookingContext.getAmount())
                        .provider(command.getProvider())
                        .build()
        );

        Payment payment = Payment.create(
                paymentId,
                command.getBookingId(),
                command.getUserId(),
                command.getProvider(),
                bookingContext.getAmount(),
                bookingContext.getCurrency(),
                paymentUrl,
                command.getIdempotencyKey(),
                command.getRequestHash(),
                bookingContext.getExpiresAt()
        );

        Payment savedPayment = savePaymentPort.save(payment);

        // 4. Outbox Event
        String payload = String.format(
                "{\"paymentId\":\"%s\",\"bookingId\":\"%s\",\"userId\":\"%s\",\"amount\":%s,\"currency\":\"%s\"}",
                savedPayment.getId(),
                savedPayment.getBookingId(),
                savedPayment.getUserId(),
                savedPayment.getAmount(),
                savedPayment.getCurrency()
        );
        saveOutboxEventPort.save(savedPayment.getId(), "PaymentInitiated", payload);

        log.info("Payment created with ID {}", savedPayment.getId());
        return savedPayment;
    }
}
