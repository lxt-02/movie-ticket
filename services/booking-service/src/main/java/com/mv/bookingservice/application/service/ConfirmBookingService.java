package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.ConfirmBookingCommand;
import com.mv.bookingservice.application.port.in.ConfirmBookingUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.exception.BookingNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfirmBookingService implements ConfirmBookingUseCase {

    private final LoadBookingPort loadBookingPort;
    private final SaveBookingPort saveBookingPort;

    @Override
    @Transactional
    public Booking execute(ConfirmBookingCommand command) {
        Booking booking = loadBookingPort.findById(command.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.getBookingId()));

        booking.confirm(command.getPaymentId());
        return saveBookingPort.save(booking);
    }
}
