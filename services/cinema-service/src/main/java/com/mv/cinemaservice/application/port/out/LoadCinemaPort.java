package com.mv.cinemaservice.application.port.out;

import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;

import java.util.Optional;
import java.util.UUID;

public interface LoadCinemaPort {
    Optional<Cinema> findById(UUID id);
}
