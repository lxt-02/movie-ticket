package com.mv.showtimeservice.domain.model.seathold.exception;

import java.util.List;
import java.util.UUID;

public class SeatUnavailableException extends DomainException {
    public SeatUnavailableException(String message) {
        super(message);
    }

    public SeatUnavailableException(List<UUID> seatIds) {
        super("Các ghế sau không còn trống: " + seatIds);
    }
}
