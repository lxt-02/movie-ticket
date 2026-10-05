package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.ConfirmBookingCommand;
import com.mv.bookingservice.application.port.in.ConfirmBookingUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.exception.BookingNotFoundException;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfirmBookingService implements ConfirmBookingUseCase {

    private final LoadBookingPort loadBookingPort;
    private final SaveBookingPort saveBookingPort;
    private final ShowtimeClientPort showtimeClientPort;

    @Override
    @Transactional
    public Booking execute(ConfirmBookingCommand command) {
        Booking booking = loadBookingPort.findById(command.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.getBookingId()));

        if (command.getAmount() != null && command.getAmount().compareTo(booking.getTotalAmount()) != 0) {
            throw new BookingValidationException("Payment amount does not match booking total");
        }
        if (command.getCurrency() != null && !command.getCurrency().equalsIgnoreCase(booking.getCurrency())) {
            throw new BookingValidationException("Payment currency does not match booking currency");
        }
        if (booking.isExpired()) {
            booking.requestRefund();
            return saveBookingPort.save(booking);
        }

        showtimeClientPort.confirmHold(booking.getHoldId());
        booking.confirm(command.getPaymentId());
        return saveBookingPort.save(booking);
    }
}
