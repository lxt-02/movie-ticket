package com.mv.bookingservice.application.port.in;

import com.mv.bookingservice.application.command.ExpireBookingCommand;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;

public interface ExpireBookingUseCase {
    Booking execute(ExpireBookingCommand command);
}
