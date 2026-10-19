package com.mv.paymentservice.application.port.in;

import com.mv.paymentservice.application.command.CreatePaymentCommand;

public interface RecordPaymentCreationFailureUseCase {
    /** Writes PAYMENT_CREATE_FAILED to the outbox in a new transaction. */
    void recordFailure(CreatePaymentCommand command, String reason, String message);
}
