package com.mv.cinemaservice.application.port.in;

import com.mv.cinemaservice.application.dto.PageResult;
import com.mv.cinemaservice.application.query.SearchCinemasQuery;
import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;

import java.util.List;
import java.util.UUID;
import com.mv.cinemaservice.application.query.GetCinemaQuery;
import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;

public interface GetCinemaUseCase {
    Cinema getById(GetCinemaQuery query);

    PageResult<Cinema> search(SearchCinemasQuery query);

    /** Screens of a cinema (404 if the cinema does not exist). */
    List<Screen> getScreens(UUID cinemaId);
}
