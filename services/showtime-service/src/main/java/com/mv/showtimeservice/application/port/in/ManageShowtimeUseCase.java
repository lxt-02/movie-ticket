package com.mv.showtimeservice.application.port.in;

import com.mv.showtimeservice.application.command.UpdateShowtimeCommand;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;

import java.util.UUID;

/** Admin changes to an existing showtime (create is {@link CreateShowtimeUseCase}). */
public interface ManageShowtimeUseCase {
    Showtime update(UUID showtimeId, UpdateShowtimeCommand command);

    /** Soft delete: CANCELLED, seats blocked. Refused while seats are held or sold. */
    Showtime cancel(UUID showtimeId);
}
