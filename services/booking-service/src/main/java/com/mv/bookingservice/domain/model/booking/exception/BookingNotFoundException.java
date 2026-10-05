package com.mv.bookingservice.domain.model.booking.exception;

import java.util.UUID;

public class BookingNotFoundException extends DomainException {
    public BookingNotFoundException(UUID bookingId) {
        super("Booking not found with id: " + bookingId);
    }

    public BookingNotFoundException(String bookingCode) {
        super("Booking not found with code: " + bookingCode);
    }
}
