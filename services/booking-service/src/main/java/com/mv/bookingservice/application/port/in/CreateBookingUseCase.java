package com.mv.bookingservice.application.port.in;

import com.mv.bookingservice.application.command.CreateBookingCommand;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;

public interface CreateBookingUseCase {
    Booking execute(CreateBookingCommand command);
}
