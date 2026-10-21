package com.mv.showtimeservice.domain.model.seathold.exception;

import com.mv.showtimeservice.domain.model.seathold.enums.SeatHoldStatus;

public class InvalidSeatHoldStateException extends DomainException {
    public InvalidSeatHoldStateException(String message) {
        super(message);
    }

    public InvalidSeatHoldStateException(SeatHoldStatus currentStatus, String targetAction) {
        super("Không thể " + label(targetAction) + " lượt giữ ghế đang ở trạng thái " + currentStatus);
    }

    private static String label(String action) {
        return switch (action) {
            case "confirm" -> "xác nhận";
            case "release" -> "nhả";
            case "expire" -> "cho hết hạn";
            default -> action;
        };
    }
}
