package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.CreateBookingCommand;
import com.mv.bookingservice.application.command.CreateBookingSeatCommand;
import com.mv.bookingservice.application.dto.BookingPaymentPolicy;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.PaymentCommandPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.application.port.out.UserClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.exception.BookingConflictException;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateBookingServiceTest {

    @Mock
    private SaveBookingPort saveBookingPort;

    @Mock
    private LoadBookingPort loadBookingPort;

    @Mock
    private ShowtimeClientPort showtimeClientPort;

    @Mock
    private UserClientPort userClientPort;

    @Mock
    private PaymentCommandPort paymentCommandPort;

    private CreateBookingService createBookingService;

    @BeforeEach
    void setUp() {
        createBookingService = new CreateBookingService(saveBookingPort, loadBookingPort, showtimeClientPort, userClientPort, paymentCommandPort,
                new BookingPaymentPolicy(3, java.time.Duration.ofSeconds(90), 50));
    }

    @Test
    void shouldCreateNewBookingSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID showtimeSeatId = UUID.randomUUID();

        CreateBookingSeatCommand seatCmd = CreateBookingSeatCommand.builder()
                .showtimeSeatId(showtimeSeatId)
                .build();

        CreateBookingCommand command = CreateBookingCommand.builder()
                .userId(userId)
                .showtimeId(showtimeId)
                .movieTitle("Movie 1")
                .cinemaName("Cinema 1")
                .screenName("Screen 1")
                .startTime(Instant.now().plusSeconds(3600))
                .holdId(holdId)
                .idempotencyKey("idem-key-1")
                .requestHash("hash-1")
                .currency("VND")
                .expiresAt(Instant.now().plusSeconds(600))
                .discountAmount(BigDecimal.ZERO)
                .seats(List.of(seatCmd))
                .build();

        when(loadBookingPort.findByUserIdAndIdempotencyKey(userId, "idem-key-1")).thenReturn(Optional.empty());
        when(userClientPort.validateUser(userId)).thenReturn(new UserClientPort.UserValidationResult(userId, "u@example.com", "User", "ACTIVE"));
        when(showtimeClientPort.holdSeats(eq(showtimeId), any(UUID.class), eq(List.of(showtimeSeatId))))
                .thenReturn(new ShowtimeClientPort.SeatHoldResult(
                        holdId,
                        Instant.now().plusSeconds(600),
                        "Movie 1",
                        "Cinema 1",
                        "Screen 1",
                        Instant.now().plusSeconds(3600),
                        List.of(new ShowtimeClientPort.HeldSeatDetail(showtimeSeatId, seatId, "A1", new BigDecimal("100000.00")))
                ));
        when(saveBookingPort.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = createBookingService.execute(command);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("idem-key-1", result.getIdempotencyKey());
        assertEquals(holdId, result.getHoldId());
        // Hold ends in 600s; the payment deadline is 180s earlier.
        org.junit.jupiter.api.Assertions.assertTrue(result.getExpiresAt().isBefore(Instant.now().plusSeconds(421)));
        org.junit.jupiter.api.Assertions.assertTrue(result.getExpiresAt().isAfter(Instant.now().plusSeconds(410)));
        verify(saveBookingPort, times(1)).save(any(Booking.class));
        verify(paymentCommandPort).requestPayment(result);
    }

    @Test
    void shouldValidateUserBeforeHoldingSeats() {
        UUID userId = UUID.randomUUID();

        CreateBookingCommand command = CreateBookingCommand.builder()
                .userId(userId)
                .idempotencyKey("idem-key-1")
                .requestHash("hash-1")
                .build();

        when(loadBookingPort.findByUserIdAndIdempotencyKey(userId, "idem-key-1")).thenReturn(Optional.empty());
        when(userClientPort.validateUser(userId)).thenReturn(new UserClientPort.UserValidationResult(userId, "u@example.com", "User", "BLOCKED"));

        assertThrows(BookingValidationException.class, () -> createBookingService.execute(command));

        verify(showtimeClientPort, never()).holdSeats(any(), any(), any());
        verify(saveBookingPort, never()).save(any());
    }

    @Test
    void shouldRequireIdempotencyKeyBeforeCallingUserOrShowtime() {
        CreateBookingCommand command = CreateBookingCommand.builder()
                .userId(UUID.randomUUID())
                .build();

        assertThrows(BookingValidationException.class, () -> createBookingService.execute(command));

        verify(userClientPort, never()).validateUser(any());
        verify(showtimeClientPort, never()).holdSeats(any(), any(), any());
        verify(saveBookingPort, never()).save(any());
    }

    @Test
    void shouldReleaseHoldWhenSavingBookingFails() {
        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID showtimeSeatId = UUID.randomUUID();

        CreateBookingSeatCommand seatCmd = CreateBookingSeatCommand.builder()
                .showtimeSeatId(showtimeSeatId)
                .build();

        CreateBookingCommand command = CreateBookingCommand.builder()
                .userId(userId)
                .showtimeId(showtimeId)
                .idempotencyKey("idem-key-1")
                .requestHash("hash-1")
                .currency("VND")
                .discountAmount(BigDecimal.ZERO)
                .seats(List.of(seatCmd))
                .build();

        when(loadBookingPort.findByUserIdAndIdempotencyKey(userId, "idem-key-1")).thenReturn(Optional.empty());
        when(userClientPort.validateUser(userId)).thenReturn(new UserClientPort.UserValidationResult(userId, "u@example.com", "User", "ACTIVE"));
        when(showtimeClientPort.holdSeats(eq(showtimeId), any(UUID.class), eq(List.of(showtimeSeatId))))
                .thenReturn(new ShowtimeClientPort.SeatHoldResult(
                        holdId,
                        Instant.now().plusSeconds(600),
                        "Movie 1",
                        "Cinema 1",
                        "Screen 1",
                        Instant.now().plusSeconds(3600),
                        List.of(new ShowtimeClientPort.HeldSeatDetail(showtimeSeatId, seatId, "A1", new BigDecimal("100000.00")))
                ));
        when(saveBookingPort.save(any(Booking.class))).thenThrow(new IllegalStateException("db failure"));

        assertThrows(IllegalStateException.class, () -> createBookingService.execute(command));

        verify(showtimeClientPort).releaseHold(holdId);
    }

    @Test
    void shouldReturnExistingBookingOnIdempotentRetryWithSameHash() {
        UUID userId = UUID.randomUUID();
        Booking existing = Booking.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .idempotencyKey("idem-key-1")
                .requestHash("hash-1")
                .build();

        CreateBookingCommand command = CreateBookingCommand.builder()
                .userId(userId)
                .idempotencyKey("idem-key-1")
                .requestHash("hash-1")
                .build();

        when(loadBookingPort.findByUserIdAndIdempotencyKey(userId, "idem-key-1")).thenReturn(Optional.of(existing));

        Booking result = createBookingService.execute(command);

        assertEquals(existing, result);
        verify(saveBookingPort, never()).save(any(Booking.class));
    }

    @Test
    void shouldThrowConflictWhenIdempotencyKeyReusedWithDifferentHash() {
        UUID userId = UUID.randomUUID();
        Booking existing = Booking.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .idempotencyKey("idem-key-1")
                .requestHash("hash-1")
                .build();

        CreateBookingCommand command = CreateBookingCommand.builder()
                .userId(userId)
                .idempotencyKey("idem-key-1")
                .requestHash("different-hash")
                .build();

        when(loadBookingPort.findByUserIdAndIdempotencyKey(userId, "idem-key-1")).thenReturn(Optional.of(existing));

        assertThrows(BookingConflictException.class, () -> createBookingService.execute(command));
        verify(saveBookingPort, never()).save(any(Booking.class));
    }

    @Test
    void sameClientKeyFromDifferentUsersMustSendDifferentBookingIdsToShowtime() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        UUID showtimeSeatId = UUID.randomUUID();

        when(loadBookingPort.findByUserIdAndIdempotencyKey(any(UUID.class), eq("same-key"))).thenReturn(Optional.empty());
        when(userClientPort.validateUser(any(UUID.class))).thenAnswer(inv ->
                new UserClientPort.UserValidationResult(inv.getArgument(0), "u@example.com", "User", "ACTIVE"));
        when(showtimeClientPort.holdSeats(eq(showtimeId), any(UUID.class), anyList()))
                .thenAnswer(inv -> new ShowtimeClientPort.SeatHoldResult(
                        UUID.randomUUID(),
                        Instant.now().plusSeconds(600),
                        "Movie 1",
                        "Cinema 1",
                        "Screen 1",
                        Instant.now().plusSeconds(3600),
                        List.of(new ShowtimeClientPort.HeldSeatDetail(showtimeSeatId, UUID.randomUUID(), "A1", new BigDecimal("100000.00")))
                ));
        when(saveBookingPort.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        createBookingService.execute(commandFor(userA, showtimeId, showtimeSeatId, "same-key"));
        createBookingService.execute(commandFor(userB, showtimeId, showtimeSeatId, "same-key"));

        ArgumentCaptor<UUID> bookingIds = ArgumentCaptor.forClass(UUID.class);
        verify(showtimeClientPort, times(2)).holdSeats(eq(showtimeId), bookingIds.capture(), anyList());
        assertNotEquals(bookingIds.getAllValues().get(0), bookingIds.getAllValues().get(1));
    }

    @Test
    void retryWithSameUserAndKeyMustReuseSameBookingId() {
        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        UUID showtimeSeatId = UUID.randomUUID();

        when(loadBookingPort.findByUserIdAndIdempotencyKey(userId, "retry-key")).thenReturn(Optional.empty());
        when(userClientPort.validateUser(userId)).thenReturn(new UserClientPort.UserValidationResult(userId, "u@example.com", "User", "ACTIVE"));
        // Simulate a timeout on the Showtime call followed by a retry with the same user + key.
        when(showtimeClientPort.holdSeats(eq(showtimeId), any(UUID.class), anyList()))
                .thenThrow(new IllegalStateException("timeout"));

        assertThrows(IllegalStateException.class,
                () -> createBookingService.execute(commandFor(userId, showtimeId, showtimeSeatId, "retry-key")));
        assertThrows(IllegalStateException.class,
                () -> createBookingService.execute(commandFor(userId, showtimeId, showtimeSeatId, "retry-key")));

        ArgumentCaptor<UUID> bookingIds = ArgumentCaptor.forClass(UUID.class);
        verify(showtimeClientPort, times(2)).holdSeats(eq(showtimeId), bookingIds.capture(), anyList());
        assertEquals(bookingIds.getAllValues().get(0), bookingIds.getAllValues().get(1));
    }

    private CreateBookingCommand commandFor(UUID userId, UUID showtimeId, UUID showtimeSeatId, String key) {
        return CreateBookingCommand.builder()
                .userId(userId)
                .showtimeId(showtimeId)
                .idempotencyKey(key)
                .requestHash("hash-" + key)
                .currency("VND")
                .discountAmount(BigDecimal.ZERO)
                .seats(List.of(CreateBookingSeatCommand.builder().showtimeSeatId(showtimeSeatId).build()))
                .build();
    }
}
