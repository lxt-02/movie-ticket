package com.mv.cinemaservice.application.port.out;

import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadScreenPort {
    Optional<Screen> findById(UUID id);

    List<Screen> findByCinemaId(UUID cinemaId);
}
