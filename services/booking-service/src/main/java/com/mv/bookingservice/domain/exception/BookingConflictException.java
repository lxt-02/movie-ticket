package com.mv.bookingservice.domain.exception;

public class BookingConflictException extends DomainException {
    public BookingConflictException(String message) {
        super(message);
    }
}
