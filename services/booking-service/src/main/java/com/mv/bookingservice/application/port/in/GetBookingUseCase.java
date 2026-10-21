package com.mv.bookingservice.application.port.in;

import com.mv.bookingservice.application.query.GetBookingByCodeQuery;
import com.mv.bookingservice.application.query.GetBookingQuery;
import com.mv.bookingservice.application.query.GetUserBookingsQuery;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;

import java.util.List;

public interface GetBookingUseCase {
    Booking getById(GetBookingQuery query);

    Booking getByCode(GetBookingByCodeQuery query);

    List<Booking> getByUserId(GetUserBookingsQuery query);
}
