package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.ExpireBookingCommand;
import com.mv.bookingservice.application.port.in.ExpireBookingUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.exception.BookingNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpireBookingService implements ExpireBookingUseCase {

    private final LoadBookingPort loadBookingPort;
    private final SaveBookingPort saveBookingPort;

    @Override
    @Transactional
    public Booking execute(ExpireBookingCommand command) {
        Booking booking = loadBookingPort.findById(command.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.getBookingId()));

        booking.expire();
        return saveBookingPort.save(booking);
    }
}
