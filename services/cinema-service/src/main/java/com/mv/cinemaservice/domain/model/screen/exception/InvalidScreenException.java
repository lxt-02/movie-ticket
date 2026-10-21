package com.mv.cinemaservice.domain.model.screen.exception;

import com.mv.cinemaservice.domain.model.cinema.exception.DomainException;

/** A screen / seat layout rule was broken by the request (HTTP 400). */
public class InvalidScreenException extends DomainException {
    public InvalidScreenException(String message) {
        super(message);
    }
}
