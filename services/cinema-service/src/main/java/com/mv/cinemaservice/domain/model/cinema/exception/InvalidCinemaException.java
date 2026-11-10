package com.mv.cinemaservice.domain.model.cinema.exception;

/** A business rule of cinemas was broken (HTTP 400). */
public class InvalidCinemaException extends DomainException {
    public InvalidCinemaException(String message) {
        super(message);
    }
}
