package com.mv.cinemaservice.application.port.out;

import com.mv.cinemaservice.domain.model.screen.entity.ScreenType;
import com.mv.cinemaservice.domain.model.screen.entity.SeatType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadReferenceDataPort {
    List<ScreenType> findAllScreenTypes();

    Optional<ScreenType> findScreenType(UUID id);

    List<SeatType> findAllSeatTypes();
}
