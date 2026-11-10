package com.mv.showtimeservice.domain.model.showtime.exception;

import com.mv.showtimeservice.domain.model.seathold.exception.DomainException;

/** The change clashes with existing data: overlapping schedule, seats already held or sold (HTTP 409). */
public class ShowtimeConflictException extends DomainException {
    public ShowtimeConflictException(String message) {
        super(message);
    }
}
