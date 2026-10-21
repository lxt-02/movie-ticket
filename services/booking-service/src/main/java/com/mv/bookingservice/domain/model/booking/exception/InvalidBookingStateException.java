package com.mv.bookingservice.domain.model.booking.exception;

import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;

public class InvalidBookingStateException extends DomainException {
    public InvalidBookingStateException(String message) {
        super(message);
    }

    public InvalidBookingStateException(BookingStatus currentStatus, String targetAction) {
        super("Không thể " + label(targetAction) + " booking đang ở trạng thái " + currentStatus);
    }

    private static String label(String action) {
        return switch (action) {
            case "cancel" -> "huỷ";
            case "confirm", "markConfirming" -> "xác nhận";
            case "expire" -> "cho hết hạn";
            case "retryPayment" -> "thanh toán lại";
            case "failAfterPayment" -> "huỷ sau thanh toán";
            default -> action;
        };
    }
}
