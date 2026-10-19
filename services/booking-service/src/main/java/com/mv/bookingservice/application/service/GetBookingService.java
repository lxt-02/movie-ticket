package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.port.in.GetBookingUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.query.GetBookingByCodeQuery;
import com.mv.bookingservice.application.query.GetBookingQuery;
import com.mv.bookingservice.application.query.GetUserBookingsQuery;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.exception.BookingNotFoundException;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetBookingService implements GetBookingUseCase {

    private final LoadBookingPort loadBookingPort;

    @Override
    public Booking getById(GetBookingQuery query) {
        Booking booking = loadBookingPort.findById(query.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(query.getBookingId()));

        requireOwner(booking, query.getUserId());
        return booking;
    }

    @Override
    public Booking getByCode(GetBookingByCodeQuery query) {
        Booking booking = loadBookingPort.findByBookingCode(query.getBookingCode())
                .orElseThrow(() -> new BookingNotFoundException(query.getBookingCode()));
        requireOwner(booking, query.getUserId());
        return booking;
    }

    @Override
    public List<Booking> getByUserId(GetUserBookingsQuery query) {
        if (query.getUserId() == null) {
            throw new BookingValidationException("userId is required");
        }
        return loadBookingPort.findByUserId(query.getUserId());
    }

    /** A missing caller is denied too; someone else's booking looks like "not found" so ids cannot be probed. */
    private void requireOwner(Booking booking, java.util.UUID userId) {
        if (userId == null || !userId.equals(booking.getUserId())) {
            throw new BookingNotFoundException(booking.getId());
        }
    }
}
