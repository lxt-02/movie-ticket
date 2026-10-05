package com.mv.movieservice.application.service;

import com.mv.movieservice.application.port.in.GetMovieUseCase;
import com.mv.movieservice.application.port.out.LoadMoviePort;
import com.mv.movieservice.application.query.GetMovieQuery;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.exception.MovieNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMovieService implements GetMovieUseCase {

    private final LoadMoviePort loadMoviePort;

    @Override
    public Movie getById(GetMovieQuery query) {
        return loadMoviePort.findById(query.getMovieId())
                .orElseThrow(() -> new MovieNotFoundException(query.getMovieId()));
    }
}
