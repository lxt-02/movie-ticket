package com.mv.movieservice.domain.model.movie.exception;

public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
