package com.mv.cinemaservice.application.service;

import com.mv.cinemaservice.application.port.in.GetCinemaUseCase;
import com.mv.cinemaservice.application.port.out.LoadCinemaPort;
import com.mv.cinemaservice.application.query.GetCinemaQuery;
import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;
import com.mv.cinemaservice.domain.model.cinema.exception.CinemaNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCinemaService implements GetCinemaUseCase {

    private final LoadCinemaPort loadCinemaPort;

    @Override
    public Cinema getById(GetCinemaQuery query) {
        return loadCinemaPort.findById(query.getCinemaId())
                .orElseThrow(() -> new CinemaNotFoundException(query.getCinemaId()));
    }
}
