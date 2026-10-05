package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.CreateBookingCommand;
import com.mv.bookingservice.application.command.CreateBookingSeatCommand;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.application.port.out.UserClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.exception.BookingConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private CreateBookingService createBookingService;

    @BeforeEach
    void setUp() {
        createBookingService = new CreateBookingService(saveBookingPort, loadBookingPort, showtimeClientPort, userClientPort);
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
        when(showtimeClientPort.holdSeats(eq(showtimeId), any(UUID.class), eq(List.of(showtimeSeatId)), eq("idem-key-1")))
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
        verify(saveBookingPort, times(1)).save(any(Booking.class));
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
}
