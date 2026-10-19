package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.ExpireBookingCommand;
import com.mv.bookingservice.application.port.in.ExpireBookingUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.PaymentCommandPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.exception.BookingNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpireBookingService implements ExpireBookingUseCase {

    private final LoadBookingPort loadBookingPort;
    private final SaveBookingPort saveBookingPort;
    private final ShowtimeClientPort showtimeClientPort;
    private final PaymentCommandPort paymentCommandPort;

    @Override
    @Transactional
    public Booking execute(ExpireBookingCommand command) {
        Booking booking = loadBookingPort.findById(command.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.getBookingId()));

        booking.expire();
        Booking saved = saveBookingPort.save(booking);
        paymentCommandPort.cancelPayments(saved, "Booking expired");
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
