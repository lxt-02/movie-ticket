package com.mv.showtimeservice.application.port.in;

import com.mv.showtimeservice.application.command.HoldSeatsCommand;

public interface HoldSeatsUseCase {
    HoldSeatsResult execute(HoldSeatsCommand command);
}
