package com.mv.bookingservice.application.port.in;

import com.mv.bookingservice.application.command.CancelBookingCommand;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;

public interface CancelBookingUseCase {
    Booking execute(CancelBookingCommand command);
}
