package com.mv.movieservice.application.port.in;

import com.mv.movieservice.application.command.SaveMovieCommand;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;

import java.util.UUID;

/** Admin operations on the movie catalogue. */
public interface ManageMovieUseCase {
    Movie create(SaveMovieCommand command);

    Movie update(UUID movieId, SaveMovieCommand command);

    Movie changeStatus(UUID movieId, MovieStatus status);

    /** Soft delete: the movie becomes ARCHIVED. */
    Movie archive(UUID movieId);
}
