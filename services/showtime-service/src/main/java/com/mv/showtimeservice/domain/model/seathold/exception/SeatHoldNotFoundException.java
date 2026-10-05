package com.mv.showtimeservice.domain.model.seathold.exception;

import java.util.UUID;

public class SeatHoldNotFoundException extends DomainException {
    public SeatHoldNotFoundException(UUID holdId) {
        super("Seat hold not found: " + holdId);
    }

    public static SeatHoldNotFoundException byBookingId(UUID bookingId) {
        return new SeatHoldNotFoundException(bookingId);
    }
}
