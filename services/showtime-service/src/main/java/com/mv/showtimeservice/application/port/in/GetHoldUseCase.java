package com.mv.showtimeservice.application.port.in;

import com.mv.showtimeservice.application.query.GetHoldByBookingIdQuery;
import com.mv.showtimeservice.application.query.GetHoldByIdQuery;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;

public interface GetHoldUseCase {
    SeatHold getById(GetHoldByIdQuery query);

    SeatHold getByBookingId(GetHoldByBookingIdQuery query);
}
