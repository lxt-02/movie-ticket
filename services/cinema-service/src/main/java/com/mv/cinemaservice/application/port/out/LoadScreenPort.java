package com.mv.cinemaservice.application.port.out;

import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;

import java.util.Optional;
import java.util.UUID;

public interface LoadScreenPort {
    Optional<Screen> findById(UUID id);
}
