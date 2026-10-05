package com.mv.movieservice.application.port.out;

import com.mv.movieservice.domain.model.movie.aggregate.Movie;

import java.util.Optional;
import java.util.UUID;

public interface LoadMoviePort {
    Optional<Movie> findById(UUID id);
}
