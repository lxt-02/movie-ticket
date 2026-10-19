package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.CreateBookingCommand;
import com.mv.bookingservice.application.command.CreateBookingSeatCommand;
import com.mv.bookingservice.application.dto.BookingPaymentPolicy;
import com.mv.bookingservice.application.port.in.CreateBookingUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.PaymentCommandPort;
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
import java.time.Instant;
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
    private final PaymentCommandPort paymentCommandPort;
    private final BookingPaymentPolicy policy;

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
        // bookingId is stable per (user, Idempotency-Key), so it doubles as the idempotency key
        // for the Showtime call. The client's key stays inside Booking.
        UUID bookingId = stableBookingId(command.getUserId(), command.getIdempotencyKey());
        ShowtimeClientPort.SeatHoldResult hold = showtimeClientPort.holdSeats(
                command.getShowtimeId(),
                bookingId,
                requestedSeatIds
        );

        try {
            List<BookingSeat> seats = toBookingSeats(hold);
            Instant paymentDeadline = paymentDeadline(hold);
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
                    paymentDeadline,
                    command.getDiscountAmount(),
                    seats
            );

            Booking saved = saveBookingPort.save(booking);
            // Same transaction: the CREATE_PAYMENT command is stored in the outbox with the booking.
            paymentCommandPort.requestPayment(saved);
            return saved;
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

    /**
     * Payment deadline = hold end - safety margin. The booking's expires_at, the PayOS link expiry and the
     * "paid in time" check all use it, so the seats stay held a few minutes after the buyer's last chance to pay
     * (a late webhook can still confirm them) and the expiry job releases them before Showtime reclaims them.
     */
    private Instant paymentDeadline(ShowtimeClientPort.SeatHoldResult hold) {
        if (hold.expiresAt() == null) {
            throw new BookingValidationException("Showtime did not return the seat hold expiry");
        }
        Instant deadline = hold.expiresAt().minus(policy.holdSafetyMargin());
        if (!deadline.isAfter(Instant.now())) {
            throw new BookingValidationException("Seat hold is too short to leave time for payment");
        }
        return deadline;
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
