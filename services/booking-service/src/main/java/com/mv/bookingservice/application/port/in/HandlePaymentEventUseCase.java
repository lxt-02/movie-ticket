package com.mv.bookingservice.application.port.in;

import com.mv.bookingservice.application.command.PaymentEventCommand;

public interface HandlePaymentEventUseCase {
    void handle(PaymentEventCommand command);
}
