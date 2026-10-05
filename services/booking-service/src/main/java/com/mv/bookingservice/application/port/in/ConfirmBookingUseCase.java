package com.mv.bookingservice.application.port.in;

import com.mv.bookingservice.application.command.ConfirmBookingCommand;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;

public interface ConfirmBookingUseCase {
    Booking execute(ConfirmBookingCommand command);
}
