package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.command.CreatePaymentCommand;
import com.mv.paymentservice.application.event.PaymentEventFactory;
import com.mv.paymentservice.application.port.in.RecordPaymentCreationFailureUseCase;
import com.mv.paymentservice.application.port.out.PaymentEventOutboxPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordPaymentCreationFailureService implements RecordPaymentCreationFailureUseCase {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private final PaymentEventOutboxPort outboxPort;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(CreatePaymentCommand command, String reason, String message) {
        String safeMessage = message == null ? null
                : message.length() > MAX_MESSAGE_LENGTH ? message.substring(0, MAX_MESSAGE_LENGTH) : message;
        outboxPort.append(PaymentEventFactory.createFailed(command, reason, safeMessage, clock.instant()));
        log.warn("PAYMENT_CREATE_FAILED for booking {}: {} - {}", command.getBookingId(), reason, safeMessage);
    }
}
