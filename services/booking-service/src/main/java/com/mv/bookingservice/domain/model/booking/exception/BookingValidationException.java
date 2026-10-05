package com.mv.bookingservice.domain.model.booking.exception;

public class BookingValidationException extends DomainException {
    public BookingValidationException(String message) {
        super(message);
    }
}
