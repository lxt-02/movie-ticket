package com.mv.cinemaservice.domain.repository;

import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;

import java.util.Optional;
import java.util.UUID;

public interface CinemaRepository {
    Optional<Cinema> findById(UUID id);
    Cinema save(Cinema cinema);
}
