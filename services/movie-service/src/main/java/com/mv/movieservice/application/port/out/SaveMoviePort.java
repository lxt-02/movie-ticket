package com.mv.movieservice.application.port.out;

import com.mv.movieservice.domain.model.movie.aggregate.Movie;

public interface SaveMoviePort {
    Movie save(Movie movie);
}
