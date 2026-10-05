package com.mv.showtimeservice.domain.model.seathold.exception;

import com.mv.showtimeservice.domain.model.seathold.enums.SeatHoldStatus;

public class InvalidSeatHoldStateException extends DomainException {
    public InvalidSeatHoldStateException(String message) {
        super(message);
    }

    public InvalidSeatHoldStateException(SeatHoldStatus currentStatus, String targetAction) {
        super("Cannot " + targetAction + " hold in status " + currentStatus);
    }
}
