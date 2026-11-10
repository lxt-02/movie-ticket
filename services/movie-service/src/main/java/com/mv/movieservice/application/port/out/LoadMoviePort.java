package com.mv.movieservice.application.port.out;

import com.mv.movieservice.application.dto.PageResult;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;

import java.util.Optional;
import java.util.UUID;

public interface LoadMoviePort {
    Optional<Movie> findById(UUID id);

    /**
     * @param status          exact status, or null for any
     * @param keyword         part of the title / original title (case-insensitive), or null
     * @param includeArchived when status is null, whether ARCHIVED movies are listed
     */
    PageResult<Movie> search(MovieStatus status, String keyword, boolean includeArchived, int page, int size);
}
