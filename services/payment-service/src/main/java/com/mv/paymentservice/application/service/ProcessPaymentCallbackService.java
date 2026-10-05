package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.command.PaymentCallbackCommand;
import com.mv.paymentservice.application.port.in.ProcessPaymentCallbackUseCase;
import com.mv.paymentservice.application.port.out.BookingConfirmationPort;
import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.application.port.out.SaveOutboxEventPort;
import com.mv.paymentservice.application.port.out.SavePaymentPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentCallbackService implements ProcessPaymentCallbackUseCase {

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final SaveOutboxEventPort saveOutboxEventPort;
    private final BookingConfirmationPort bookingConfirmationPort;

    @Override
    @Transactional
    public Payment processCallback(PaymentCallbackCommand command) {
        Payment payment = loadPaymentPort.findById(command.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.getPaymentId()));

        if (command.isSuccess()) {
            payment.markSuccess(command.getProviderTransactionId(), Instant.now());
            Payment savedPayment = savePaymentPort.save(payment);

            String payload = String.format(
                    "{\"paymentId\":\"%s\",\"bookingId\":\"%s\",\"providerTransactionId\":\"%s\",\"status\":\"SUCCESS\"}",
                    savedPayment.getId(),
                    savedPayment.getBookingId(),
                    savedPayment.getProviderTransactionId()
            );
            saveOutboxEventPort.save(savedPayment.getId(), "PaymentSucceeded", payload);
            bookingConfirmationPort.confirmPaymentSucceeded(
                    savedPayment.getBookingId(),
                    savedPayment.getId(),
                    savedPayment.getAmount(),
                    savedPayment.getCurrency()
            );
            log.info("Payment {} marked as SUCCESS", savedPayment.getId());
            return savedPayment;
        } else {
            payment.markFailed(command.getFailureReason());
            Payment savedPayment = savePaymentPort.save(payment);

            String payload = String.format(
                    "{\"paymentId\":\"%s\",\"bookingId\":\"%s\",\"status\":\"FAILED\",\"reason\":\"%s\"}",
                    savedPayment.getId(),
                    savedPayment.getBookingId(),
                    command.getFailureReason() != null ? command.getFailureReason() : "Payment gateway failure"
            );
            saveOutboxEventPort.save(savedPayment.getId(), "PaymentFailed", payload);
            log.info("Payment {} marked as FAILED", savedPayment.getId());
            return savedPayment;
        }
    }
}
