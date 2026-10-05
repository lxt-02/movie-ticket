package com.mv.bookingservice.domain.model.booking.aggregate;

import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import com.mv.bookingservice.domain.model.booking.exception.InvalidBookingStateException;
import com.mv.bookingservice.domain.model.booking.entity.BookingSeat;
import com.mv.bookingservice.domain.model.booking.enums.BookingSeatStatus;
import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BookingAggregateTest {

    @Test
    void shouldCreateBookingSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID showtimeSeatId = UUID.randomUUID();

        BookingSeat seat = BookingSeat.create(null, showtimeSeatId, seatId, "A1", new BigDecimal("100000.00"));

        Booking booking = Booking.create(
                userId,
                showtimeId,
                "Inception",
                "Cinema Center",
                "Screen 1",
                Instant.now().plusSeconds(3600),
                holdId,
                "idempotency-123",
                "hash-123",
                "VND",
                Instant.now().plusSeconds(600),
                BigDecimal.ZERO,
                List.of(seat)
        );

        assertNotNull(booking.getId());
        assertNotNull(booking.getBookingCode());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(new BigDecimal("100000.00"), booking.getSubtotal());
        assertEquals(new BigDecimal("100000.00"), booking.getTotalAmount());
        assertEquals(1, booking.getSeats().size());
        assertNotNull(booking.getSeats().get(0).getTicketCode());
    }

    @Test
    void shouldThrowExceptionWhenMissingRequiredFields() {
        assertThrows(BookingValidationException.class, () -> Booking.create(
                null,
                UUID.randomUUID(),
                "Title",
                "Cinema",
                "Screen",
                Instant.now(),
                UUID.randomUUID(),
                "key",
                "hash",
                "VND",
                Instant.now().plusSeconds(600),
                BigDecimal.ZERO,
                List.of()
        ));
    }

    @Test
    void shouldTransitionStatusCorrectly() {
        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        BookingSeat seat = BookingSeat.create(null, UUID.randomUUID(), UUID.randomUUID(), "A1", new BigDecimal("100000.00"));

        Booking booking = Booking.create(
                userId,
                showtimeId,
                "Inception",
                "Cinema Center",
                "Screen 1",
                Instant.now().plusSeconds(3600),
                holdId,
                "idempotency-123",
                "hash-123",
                "VND",
                Instant.now().plusSeconds(600),
                BigDecimal.ZERO,
                List.of(seat)
        );

        UUID paymentId = UUID.randomUUID();
        booking.markConfirming(paymentId);
        assertEquals(BookingStatus.CONFIRMING, booking.getStatus());
        assertEquals(paymentId, booking.getPaidPaymentId());

        booking.confirm(paymentId);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertNotNull(booking.getConfirmedAt());
    }

    @Test
    void shouldRejectDuplicateShowtimeSeatInsideBooking() {
        UUID showtimeSeatId = UUID.randomUUID();

        BookingSeat firstSeat = BookingSeat.create(
                null,
                showtimeSeatId,
                UUID.randomUUID(),
                "A1",
                new BigDecimal("100000.00")
        );
        BookingSeat duplicateSeat = BookingSeat.create(
                null,
                showtimeSeatId,
                UUID.randomUUID(),
                "A2",
                new BigDecimal("100000.00")
        );

        assertThrows(BookingValidationException.class, () -> Booking.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Inception",
                "Cinema Center",
                "Screen 1",
                Instant.now().plusSeconds(3600),
                UUID.randomUUID(),
                "idempotency-123",
                "hash-123",
                "VND",
                Instant.now().plusSeconds(600),
                BigDecimal.ZERO,
                List.of(firstSeat, duplicateSeat)
        ));
    }

    @Test
    void shouldMoveSeatStatusWithBookingStatus() {
        BookingSeat seat = BookingSeat.create(
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "A1",
                new BigDecimal("100000.00")
        );
        Booking booking = Booking.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Inception",
                "Cinema Center",
                "Screen 1",
                Instant.now().plusSeconds(3600),
                UUID.randomUUID(),
                "idempotency-123",
                "hash-123",
                "VND",
                Instant.now().plusSeconds(600),
                BigDecimal.ZERO,
                List.of(seat)
        );

        booking.cancel();

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(BookingSeatStatus.CANCELLED, booking.getSeats().get(0).getStatus());
    }
}
