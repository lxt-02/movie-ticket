package com.mv.cinemaservice.domain.model.screen.exception;

import com.mv.cinemaservice.domain.model.cinema.exception.DomainException;

/** Clashes with existing data, e.g. a screen name already used in the cinema (HTTP 409). */
public class ScreenConflictException extends DomainException {
    public ScreenConflictException(String message) {
        super(message);
    }
}
