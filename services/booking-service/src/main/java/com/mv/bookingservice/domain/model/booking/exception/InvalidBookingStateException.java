package com.mv.bookingservice.domain.model.booking.exception;

import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;

public class InvalidBookingStateException extends DomainException {
    public InvalidBookingStateException(String message) {
        super(message);
    }

    public InvalidBookingStateException(BookingStatus currentStatus, String targetAction) {
        super("Cannot perform " + targetAction + " on booking with status " + currentStatus);
    }
}
