package com.mv.cinemaservice.application.port.out;

import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;

public interface SaveCinemaPort {
    Cinema save(Cinema cinema);
}
