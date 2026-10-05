package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.CreateBookingCommand;
import com.mv.bookingservice.application.command.CreateBookingSeatCommand;
import com.mv.bookingservice.application.port.in.CreateBookingUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.application.port.out.UserClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.entity.BookingSeat;
import com.mv.bookingservice.domain.model.booking.exception.BookingConflictException;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreateBookingService implements CreateBookingUseCase {

    private final SaveBookingPort saveBookingPort;
    private final LoadBookingPort loadBookingPort;
    private final ShowtimeClientPort showtimeClientPort;
    private final UserClientPort userClientPort;

    @Override
    @Transactional
    public Booking execute(CreateBookingCommand command) {
        if (command == null) {
            throw new BookingValidationException("Create booking command must not be null");
        }
        if (command.getUserId() == null) {
            throw new BookingValidationException("userId must be provided by authenticated context");
        }
        if (command.getIdempotencyKey() == null || command.getIdempotencyKey().isBlank()) {
            throw new BookingValidationException("Idempotency-Key header is required when creating a booking");
        }

        Optional<Booking> existingBooking = loadBookingPort.findByUserIdAndIdempotencyKey(
                command.getUserId(),
                command.getIdempotencyKey()
        );
        if (existingBooking.isPresent()) {
            Booking existing = existingBooking.get();
            if (command.getRequestHash() != null && command.getRequestHash().equals(existing.getRequestHash())) {
                return existing;
            }
            throw new BookingConflictException("Idempotency key reused with different request payload");
        }

        UserClientPort.UserValidationResult user = userClientPort.validateUser(command.getUserId());
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new BookingValidationException("User is not active");
        }

        List<UUID> requestedSeatIds = extractShowtimeSeatIds(command.getSeats());
        UUID bookingId = stableBookingId(command.getUserId(), command.getIdempotencyKey());
        ShowtimeClientPort.SeatHoldResult hold = showtimeClientPort.holdSeats(
                command.getShowtimeId(),
                bookingId,
                requestedSeatIds,
                command.getIdempotencyKey()
        );

        try {
            List<BookingSeat> seats = toBookingSeats(hold);
            Booking booking = Booking.create(
                    bookingId,
                    command.getUserId(),
                    command.getShowtimeId(),
                    hold.movieTitle(),
                    hold.cinemaName(),
                    hold.screenName(),
                    hold.startTime(),
                    hold.holdId(),
                    command.getIdempotencyKey(),
                    command.getRequestHash(),
                    command.getCurrency(),
                    hold.expiresAt(),
                    command.getDiscountAmount(),
                    seats
            );

            return saveBookingPort.save(booking);
        } catch (RuntimeException ex) {
            if (hold != null && hold.holdId() != null) {
                try {
                    showtimeClientPort.releaseHold(hold.holdId());
                } catch (RuntimeException ignored) {
                    // The hold expires in Showtime; preserve the original booking failure.
                }
            }
            throw ex;
        }
    }

    private List<UUID> extractShowtimeSeatIds(List<CreateBookingSeatCommand> seats) {
        if (seats == null || seats.isEmpty()) {
            throw new BookingValidationException("Booking must contain at least one seat");
        }
        return seats.stream()
                .map(CreateBookingSeatCommand::getShowtimeSeatId)
                .distinct()
                .collect(Collectors.toList());
    }

    private UUID stableBookingId(UUID userId, String idempotencyKey) {
        String seed = "booking:" + userId + ":" + idempotencyKey;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private List<BookingSeat> toBookingSeats(ShowtimeClientPort.SeatHoldResult hold) {
        if (hold == null || hold.holdId() == null) {
            throw new BookingValidationException("Showtime did not return a valid seat hold");
        }
        if (hold.seats() == null || hold.seats().isEmpty()) {
            throw new BookingValidationException("Showtime did not return held seats");
        }

        List<BookingSeat> seats = new ArrayList<>();
        for (ShowtimeClientPort.HeldSeatDetail seatDetail : hold.seats()) {
            seats.add(BookingSeat.create(
                    null,
                    seatDetail.showtimeSeatId(),
                    seatDetail.seatId(),
                    seatDetail.seatLabel(),
                    seatDetail.price()
            ));
        }
        return seats;
    }
}
