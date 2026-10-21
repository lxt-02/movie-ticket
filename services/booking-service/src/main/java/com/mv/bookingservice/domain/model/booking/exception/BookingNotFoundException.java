package com.mv.bookingservice.domain.model.booking.exception;

import java.util.UUID;

public class BookingNotFoundException extends DomainException {
    public BookingNotFoundException(UUID bookingId) {
        super("Không tìm thấy booking có id: " + bookingId);
    }

    public BookingNotFoundException(String bookingCode) {
        super("Không tìm thấy booking có mã: " + bookingCode);
    }
}
