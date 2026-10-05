package com.mv.paymentservice.application.port.in;

import com.mv.paymentservice.application.command.CreatePaymentCommand;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;

public interface CreatePaymentUseCase {
    Payment createPayment(CreatePaymentCommand command);
}
