package com.mv.showtimeservice.domain.model.showtime.exception;

import com.mv.showtimeservice.domain.model.seathold.exception.DomainException;

import java.util.UUID;

public class ShowtimeNotFoundException extends DomainException {
    public ShowtimeNotFoundException(UUID showtimeId) {
        super("Showtime not found: " + showtimeId);
    }
}
