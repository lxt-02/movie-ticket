package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.CancelBookingCommand;
import com.mv.bookingservice.application.port.in.CancelBookingUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.exception.BookingNotFoundException;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelBookingService implements CancelBookingUseCase {

    private final LoadBookingPort loadBookingPort;
    private final SaveBookingPort saveBookingPort;

    @Override
    @Transactional
    public Booking execute(CancelBookingCommand command) {
        Booking booking = loadBookingPort.findById(command.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.getBookingId()));

        if (command.getUserId() != null && !command.getUserId().equals(booking.getUserId())) {
            throw new BookingValidationException("Access denied to cancel this booking");
        }

        booking.cancel();
        return saveBookingPort.save(booking);
    }
}
