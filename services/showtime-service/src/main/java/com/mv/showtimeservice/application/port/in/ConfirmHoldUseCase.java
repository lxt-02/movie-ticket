package com.mv.showtimeservice.application.port.in;

import com.mv.showtimeservice.application.command.ConfirmHoldCommand;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;

public interface ConfirmHoldUseCase {
    SeatHold execute(ConfirmHoldCommand command);
}
