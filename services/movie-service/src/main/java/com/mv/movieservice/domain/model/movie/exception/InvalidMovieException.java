package com.mv.movieservice.domain.model.movie.exception;

/** A business rule of the movie catalogue was broken (HTTP 400). */
public class InvalidMovieException extends DomainException {
    public InvalidMovieException(String message) {
        super(message);
    }
}
