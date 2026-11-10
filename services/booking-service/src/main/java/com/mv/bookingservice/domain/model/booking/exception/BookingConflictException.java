package com.mv.bookingservice.domain.model.booking.exception;

public class BookingConflictException extends DomainException {
    public BookingConflictException(String message) {
        super(message);
    }
}
