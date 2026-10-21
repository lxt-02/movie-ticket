package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.CancelBookingCommand;
import com.mv.bookingservice.application.port.in.CancelBookingUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.PaymentCommandPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.exception.BookingNotFoundException;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelBookingService implements CancelBookingUseCase {

    private final LoadBookingPort loadBookingPort;
    private final SaveBookingPort saveBookingPort;
    private final ShowtimeClientPort showtimeClientPort;
    private final PaymentCommandPort paymentCommandPort;

    @Override
    @Transactional
    public Booking execute(CancelBookingCommand command) {
        Booking booking = loadBookingPort.findById(command.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.getBookingId()));

        if (command.getUserId() == null || !command.getUserId().equals(booking.getUserId())) {
            throw new BookingValidationException("Bạn không có quyền huỷ booking này");
        }

        booking.cancel();
        Booking saved = saveBookingPort.save(booking);
        paymentCommandPort.cancelPayments(saved, command.getReason() != null ? command.getReason() : "Booking cancelled");
        releaseHoldQuietly(saved);
        return saved;
    }

    /**
     * Seats are released best effort: a Showtime outage or an already expired hold must not undo the booking
     * change (the hold also ends on its own in Showtime).
     */
    private void releaseHoldQuietly(Booking booking) {
        if (booking.getHoldId() == null) {
            return;
        }
        try {
            showtimeClientPort.releaseHold(booking.getHoldId());
        } catch (RuntimeException e) {
            log.warn("Could not release hold {} of booking {}: {}", booking.getHoldId(), booking.getId(), e.getMessage());
        }
    }
}
