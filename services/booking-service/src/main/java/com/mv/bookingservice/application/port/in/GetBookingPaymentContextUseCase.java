package com.mv.bookingservice.application.port.in;

import com.mv.bookingservice.application.query.GetBookingPaymentContextQuery;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;

public interface GetBookingPaymentContextUseCase {
    Booking execute(GetBookingPaymentContextQuery query);
}
