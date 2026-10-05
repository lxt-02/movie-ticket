package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.CreateBookingCommand;
import com.mv.bookingservice.application.command.CreateBookingSeatCommand;
import com.mv.bookingservice.application.port.in.CreateBookingUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.entity.BookingSeat;
import com.mv.bookingservice.domain.model.booking.exception.BookingConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateBookingService implements CreateBookingUseCase {

    private final SaveBookingPort saveBookingPort;
    private final LoadBookingPort loadBookingPort;

    @Override
    @Transactional
    public Booking execute(CreateBookingCommand command) {
        // 1. Check Idempotency
        if (command.getIdempotencyKey() != null && !command.getIdempotencyKey().isBlank()) {
            Optional<Booking> existingBooking = loadBookingPort.findByUserIdAndIdempotencyKey(
                    command.getUserId(),
                    command.getIdempotencyKey()
            );

            if (existingBooking.isPresent()) {
                Booking existing = existingBooking.get();
                if (command.getRequestHash() != null && command.getRequestHash().equals(existing.getRequestHash())) {
                    return existing;
                } else {
                    throw new BookingConflictException("Idempotency key reused with different request payload");
                }
            }
        }

        // 2. Check holdId uniqueness if holdId present
        if (command.getHoldId() != null) {
            Optional<Booking> existingHoldBooking = loadBookingPort.findByHoldId(command.getHoldId());
            if (existingHoldBooking.isPresent()) {
                throw new BookingConflictException("Hold ID is already bound to an existing booking: " + command.getHoldId());
            }
        }

        // 3. Map seat commands to domain items
        List<BookingSeat> seats = new ArrayList<>();
        if (command.getSeats() != null) {
            for (CreateBookingSeatCommand seatCmd : command.getSeats()) {
                BookingSeat seat = BookingSeat.create(
                        null,
                        seatCmd.getShowtimeSeatId(),
                        seatCmd.getSeatId(),
                        seatCmd.getSeatLabel(),
                        seatCmd.getUnitPrice()
                );
                seats.add(seat);
            }
        }

        // 4. Create aggregate (validates domain invariants)
        Booking booking = Booking.create(
                command.getUserId(),
                command.getShowtimeId(),
                command.getMovieTitle(),
                command.getCinemaName(),
                command.getScreenName(),
                command.getStartTime(),
                command.getHoldId(),
                command.getIdempotencyKey(),
                command.getRequestHash(),
                command.getCurrency(),
                command.getExpiresAt(),
                command.getDiscountAmount(),
                seats
        );

        // 5. Persist through output port
        return saveBookingPort.save(booking);
    }
}
