package com.mv.cinemaservice.application.port.in;

import com.mv.cinemaservice.application.query.GetCinemaQuery;
import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;

public interface GetCinemaUseCase {
    Cinema getById(GetCinemaQuery query);
}
