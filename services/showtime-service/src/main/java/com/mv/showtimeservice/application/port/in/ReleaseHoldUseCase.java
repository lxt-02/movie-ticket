package com.mv.showtimeservice.application.port.in;

import com.mv.showtimeservice.application.command.ReleaseHoldCommand;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;

public interface ReleaseHoldUseCase {
    SeatHold execute(ReleaseHoldCommand command);
}
