package com.mv.cinemaservice.application.service;

import com.mv.cinemaservice.application.port.in.GetCinemaUseCase;
import com.mv.cinemaservice.application.port.out.LoadCinemaPort;
import com.mv.cinemaservice.application.query.GetCinemaQuery;
import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;
import com.mv.cinemaservice.domain.model.cinema.exception.CinemaNotFoundException;
import lombok.RequiredArgsConstructor;
import com.mv.cinemaservice.application.dto.PageResult;
import com.mv.cinemaservice.application.port.out.LoadScreenPort;
import com.mv.cinemaservice.application.query.SearchCinemasQuery;
import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCinemaService implements GetCinemaUseCase {

    private final LoadCinemaPort loadCinemaPort;
    private final LoadScreenPort loadScreenPort;

    @Override
    public Cinema getById(GetCinemaQuery query) {
        return loadCinemaPort.findById(query.getCinemaId())
                .orElseThrow(() -> new CinemaNotFoundException(query.getCinemaId()));
    }

    @Override
    public PageResult<Cinema> search(SearchCinemasQuery query) {
        int page = Math.max(0, query.getPage());
        int size = Math.min(100, Math.max(1, query.getSize()));
        return loadCinemaPort.search(blankToNull(query.getCity()), query.getStatus(), blankToNull(query.getKeyword()),
                query.isIncludeClosed(), page, size);
    }

    @Override
    public List<Screen> getScreens(UUID cinemaId) {
        if (loadCinemaPort.findById(cinemaId).isEmpty()) {
            throw new CinemaNotFoundException(cinemaId);
        }
        return loadScreenPort.findByCinemaId(cinemaId);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
