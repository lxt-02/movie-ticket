package com.mv.showtimeservice.application.port.out;

import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;

public interface SaveSeatHoldPort {
    SeatHold save(SeatHold hold);
}
