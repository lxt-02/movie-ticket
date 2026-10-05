package com.mv.movieservice.domain.repository;

import com.mv.movieservice.domain.model.movie.aggregate.Movie;

import java.util.Optional;
import java.util.UUID;

public interface MovieRepository {
    Optional<Movie> findById(UUID id);
    Movie save(Movie movie);
}
