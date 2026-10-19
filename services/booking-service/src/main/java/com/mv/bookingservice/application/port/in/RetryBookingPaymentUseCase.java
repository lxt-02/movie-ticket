package com.mv.bookingservice.application.port.in;

import com.mv.bookingservice.application.command.RetryBookingPaymentCommand;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;

public interface RetryBookingPaymentUseCase {
    Booking execute(RetryBookingPaymentCommand command);
}
