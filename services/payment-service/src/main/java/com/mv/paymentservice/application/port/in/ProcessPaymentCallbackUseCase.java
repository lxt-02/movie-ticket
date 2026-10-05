package com.mv.paymentservice.application.port.in;

import com.mv.paymentservice.application.command.PaymentCallbackCommand;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;

public interface ProcessPaymentCallbackUseCase {
    Payment processCallback(PaymentCallbackCommand command);
}
