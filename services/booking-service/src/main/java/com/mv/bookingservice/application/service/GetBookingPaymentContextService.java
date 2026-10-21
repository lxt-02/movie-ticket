package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.port.in.GetBookingPaymentContextUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.query.GetBookingPaymentContextQuery;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.exception.BookingNotFoundException;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import com.mv.bookingservice.domain.model.booking.exception.InvalidBookingStateException;
import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetBookingPaymentContextService implements GetBookingPaymentContextUseCase {

    private final LoadBookingPort loadBookingPort;

    @Override
    public Booking execute(GetBookingPaymentContextQuery query) {
        Booking booking = loadBookingPort.findById(query.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(query.getBookingId()));

        if (query.getUserId() != null && !query.getUserId().equals(booking.getUserId())) {
            throw new BookingValidationException("Booking này không phải của bạn");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingStateException("Booking không còn chờ thanh toán. Trạng thái hiện tại: " + booking.getStatus());
        }

        if (booking.isExpired()) {
            throw new InvalidBookingStateException("Booking đã hết hạn lúc: " + booking.getExpiresAt());
        }

        return booking;
    }
}
