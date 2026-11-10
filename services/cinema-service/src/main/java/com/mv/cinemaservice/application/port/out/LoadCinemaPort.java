package com.mv.cinemaservice.application.port.out;

import com.mv.cinemaservice.application.dto.PageResult;
import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;
import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;

import java.util.Optional;
import java.util.UUID;

public interface LoadCinemaPort {
    Optional<Cinema> findById(UUID id);

    PageResult<Cinema> search(String city, CinemaStatus status, String keyword, boolean includeClosed,
                              int page, int size);
}
