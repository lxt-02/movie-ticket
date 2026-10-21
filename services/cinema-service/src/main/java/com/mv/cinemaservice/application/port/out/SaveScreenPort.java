package com.mv.cinemaservice.application.port.out;

import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;

import java.util.UUID;

public interface SaveScreenPort {
    Screen save(Screen screen);

    /** Case-insensitive name check inside one cinema; {@code excludeScreenId} may be null. */
    boolean existsByName(UUID cinemaId, String name, UUID excludeScreenId);
}
