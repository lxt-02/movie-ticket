package com.mv.movieservice.application.port.in;

import com.mv.movieservice.application.query.GetMovieQuery;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;

public interface GetMovieUseCase {
    Movie getById(GetMovieQuery query);
}
