package com.mv.showtimeservice.domain.repository;

import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;

import java.util.Optional;
import java.util.UUID;

public interface ShowtimeRepository {
    Optional<Showtime> findById(UUID id);
    Optional<Showtime> findByIdWithLock(UUID id);
    Showtime save(Showtime showtime);
}
