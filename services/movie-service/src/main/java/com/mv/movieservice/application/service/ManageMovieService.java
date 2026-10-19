package com.mv.movieservice.application.service;

import com.mv.movieservice.application.command.SaveMovieCommand;
import com.mv.movieservice.application.port.in.ManageMovieUseCase;
import com.mv.movieservice.application.port.out.LoadMoviePort;
import com.mv.movieservice.application.port.out.SaveMoviePort;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import com.mv.movieservice.domain.model.movie.exception.InvalidMovieException;
import com.mv.movieservice.domain.model.movie.exception.MovieNotFoundException;
import com.mv.movieservice.domain.model.movie.valueobject.MovieDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ManageMovieService implements ManageMovieUseCase {

    private final LoadMoviePort loadMoviePort;
    private final SaveMoviePort saveMoviePort;
    private final Clock clock;

    @Override
    public Movie create(SaveMovieCommand command) {
        Movie movie = Movie.create(toDetails(command), command.getStatus(), now());
        return saveMoviePort.save(movie);
    }

    @Override
    public Movie update(UUID movieId, SaveMovieCommand command) {
        Movie movie = load(movieId);
        movie.update(toDetails(command), now());
        return saveMoviePort.save(movie);
    }

    @Override
    public Movie changeStatus(UUID movieId, MovieStatus status) {
        Movie movie = load(movieId);
        movie.changeStatus(status, now());
        return saveMoviePort.save(movie);
    }

    @Override
    public Movie archive(UUID movieId) {
        Movie movie = load(movieId);
        movie.archive(now());
        return saveMoviePort.save(movie);
    }

    private Movie load(UUID movieId) {
        return loadMoviePort.findById(movieId).orElseThrow(() -> new MovieNotFoundException(movieId));
    }

    private Instant now() {
        return clock.instant();
    }

    private static MovieDetails toDetails(SaveMovieCommand c) {
        if (c == null) {
            throw new InvalidMovieException("movie data must not be null");
        }
        if (c.getDurationMinutes() == null) {
            throw new InvalidMovieException("durationMinutes must not be null");
        }
        return new MovieDetails(c.getTitle(), c.getOriginalTitle(), c.getDescription(), c.getDurationMinutes(),
                c.getReleaseDate(), c.getAgeRating(), c.getLanguage(), c.getCountry(), c.getPosterUrl());
    }
}
