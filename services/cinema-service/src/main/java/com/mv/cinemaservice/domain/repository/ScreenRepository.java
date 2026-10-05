package com.mv.cinemaservice.domain.repository;

import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;

import java.util.Optional;
import java.util.UUID;

public interface ScreenRepository {
    Optional<Screen> findById(UUID id);
    Screen save(Screen screen);
}
