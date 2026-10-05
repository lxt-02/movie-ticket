package com.mv.bookingservice.domain.exception;

import com.mv.bookingservice.domain.model.BookingStatus;

public class InvalidBookingStateException extends DomainException {
    public InvalidBookingStateException(String message) {
        super(message);
    }

    public InvalidBookingStateException(BookingStatus currentStatus, String targetAction) {
        super("Cannot perform " + targetAction + " on booking with status " + currentStatus);
    }
}
