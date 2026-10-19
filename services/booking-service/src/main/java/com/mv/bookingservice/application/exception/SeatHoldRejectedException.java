package com.mv.bookingservice.application.exception;

/**
 * Showtime refused to confirm a hold for good (409 / 404: hold released or expired, seat taken by someone else).
 * Unlike a timeout or a 5xx, retrying cannot succeed, so a paid booking has to be refunded.
 */
public class SeatHoldRejectedException extends RuntimeException {
    public SeatHoldRejectedException(String message) {
        super(message);
    }

    public SeatHoldRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
