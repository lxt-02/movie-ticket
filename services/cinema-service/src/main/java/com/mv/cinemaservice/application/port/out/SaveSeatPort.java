package com.mv.cinemaservice.application.port.out;

import com.mv.cinemaservice.domain.model.screen.entity.Seat;

import java.util.List;
import java.util.UUID;

public interface SaveSeatPort {
    /** Deletes the current seats of the screen and stores {@code seats}. */
    List<Seat> replaceSeats(UUID screenId, List<Seat> seats);

    Seat save(Seat seat);
}
