package com.mv.cinemaservice.application.port.in;

import com.mv.cinemaservice.domain.model.screen.entity.ScreenType;
import com.mv.cinemaservice.domain.model.screen.entity.SeatType;

import java.util.List;

/** Lookup lists for the admin screen editor. */
public interface GetReferenceDataUseCase {
    List<ScreenType> screenTypes();

    List<SeatType> seatTypes();
}
