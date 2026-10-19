package com.mv.showtimeservice.domain.model.showtime.exception;

import com.mv.showtimeservice.domain.model.seathold.exception.DomainException;

/** A showtime rule was broken by the request itself (HTTP 400). */
public class InvalidShowtimeException extends DomainException {
    public InvalidShowtimeException(String message) {
        super(message);
    }
}
