package com.mv.bookingservice.application.port.out;

import com.mv.bookingservice.domain.model.booking.aggregate.Booking;

public interface SaveBookingPort {
    Booking save(Booking booking);
}
