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

        if (query.getUserId() != null && !query.getUserId().equals(booking.getUserId())) {
            throw new BookingValidationException("Access denied to requested booking");
        }

        return booking;
    }

    @Override
    public Booking getByCode(GetBookingByCodeQuery query) {
        return loadBookingPort.findByBookingCode(query.getBookingCode())
                .orElseThrow(() -> new BookingNotFoundException(query.getBookingCode()));
    }

    @Override
    public List<Booking> getByUserId(GetUserBookingsQuery query) {
        return loadBookingPort.findByUserId(query.getUserId());
    }
}
