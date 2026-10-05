package com.mv.showtimeservice.application.port.out;

import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;

import java.util.Optional;
import java.util.UUID;

public interface ShowtimeRepositoryPort {
    Optional<Showtime> findById(UUID id);

    Optional<Showtime> findByIdWithLock(UUID id);
}
