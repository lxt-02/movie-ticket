package com.mv.showtimeservice.application.port.in;

import com.mv.showtimeservice.application.command.CreateShowtimeCommand;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;

public interface CreateShowtimeUseCase {
    Showtime execute(CreateShowtimeCommand command);
}
