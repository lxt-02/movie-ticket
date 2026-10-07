package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.command.HoldSeatsCommand;
import com.mv.showtimeservice.application.port.in.HoldSeatsResult;
import com.mv.showtimeservice.application.port.out.LoadSeatHoldPort;
import com.mv.showtimeservice.application.port.out.SaveSeatHoldPort;
import com.mv.showtimeservice.application.port.out.ShowtimeRepositoryPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSeatRepositoryPort;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import com.mv.showtimeservice.domain.model.seathold.enums.SeatHoldStatus;
import com.mv.showtimeservice.domain.model.seathold.exception.SeatHoldConflictException;
import com.mv.showtimeservice.domain.model.seathold.exception.SeatUnavailableException;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeSeatStatus;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeStatus;
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
class HoldSeatsServiceTest {

    @Mock
    private ShowtimeRepositoryPort showtimeRepositoryPort;

    @Mock
    private ShowtimeSeatRepositoryPort showtimeSeatRepositoryPort;

    @Mock
    private SaveSeatHoldPort saveSeatHoldPort;

    @Mock
    private LoadSeatHoldPort loadSeatHoldPort;

    private HoldSeatsService holdSeatsService;

    @BeforeEach
    void setUp() {
        holdSeatsService = new HoldSeatsService(
                showtimeRepositoryPort,
                showtimeSeatRepositoryPort,
                saveSeatHoldPort,
                loadSeatHoldPort
        );
    }

    @Test
    void shouldHoldSeatsSuccessfully() {
        UUID showtimeId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID seat1Id = UUID.randomUUID();

        Showtime showtime = Showtime.builder()
                .id(showtimeId)
                .movieTitle("Avatar 2")
                .cinemaName("Cinema 1")
                .screenName("Screen 1")
                .startTime(Instant.now().plusSeconds(3600))
                .status(ShowtimeStatus.SELLING)
                .build();

        ShowtimeSeat seat1 = ShowtimeSeat.builder()
                .id(seat1Id)
                .showtimeId(showtimeId)
                .seatId(UUID.randomUUID())
                .seatLabel("A1")
                .price(new BigDecimal("100000.00"))
                .status(ShowtimeSeatStatus.AVAILABLE)
                .build();

        HoldSeatsCommand command = HoldSeatsCommand.builder()
                .showtimeId(showtimeId)
                .bookingId(bookingId)
                .showtimeSeatIds(List.of(seat1Id))
                .idempotencyKey("key-1")
                .requestHash("hash-1")
                .holdDurationSeconds(600)
                .build();

        when(loadSeatHoldPort.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(loadSeatHoldPort.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(showtimeRepositoryPort.findByIdWithLock(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepositoryPort.findByIdsWithLock(anyList())).thenReturn(List.of(seat1));
        when(saveSeatHoldPort.save(any(SeatHold.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(showtimeSeatRepositoryPort.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        HoldSeatsResult result = holdSeatsService.execute(command);

        assertNotNull(result);
        assertEquals(SeatHoldStatus.HELD, result.getSeatHold().getStatus());
        assertEquals(bookingId, result.getSeatHold().getBookingId());
        assertEquals(1, result.getSeats().size());
        assertEquals(ShowtimeSeatStatus.HELD, result.getSeats().get(0).getStatus());
    }

    @Test
    void shouldReturnExistingHoldOnIdempotentRetryWithSameHash() {
        UUID showtimeId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        UUID showtimeSeatId = UUID.randomUUID();

        SeatHold existingHold = SeatHold.builder()
                .id(holdId)
                .bookingId(bookingId)
                .showtimeId(showtimeId)
                .idempotencyKey("key-1")
                .requestHash("hash-1")
                .status(SeatHoldStatus.HELD)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        Showtime showtime = Showtime.builder()
                .id(showtimeId)
                .movieTitle("Avatar 2")
                .cinemaName("Cinema 1")
                .screenName("Screen 1")
                .startTime(Instant.now().plusSeconds(3600))
                .status(ShowtimeStatus.SELLING)
                .build();
        ShowtimeSeat heldSeat = ShowtimeSeat.builder()
                .id(showtimeSeatId)
                .showtimeId(showtimeId)
                .status(ShowtimeSeatStatus.HELD)
                .holdId(holdId)
                .build();
        HoldSeatsCommand command = HoldSeatsCommand.builder()
                .showtimeId(showtimeId)
                .bookingId(bookingId)
                .showtimeSeatIds(List.of(showtimeSeatId))
                .idempotencyKey("key-1")
                .requestHash("hash-1")
                .holdDurationSeconds(600)
                .build();

        when(loadSeatHoldPort.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existingHold));
        when(showtimeRepositoryPort.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepositoryPort.findByHoldId(holdId)).thenReturn(List.of(heldSeat));

        HoldSeatsResult result = holdSeatsService.execute(command);

        assertEquals(existingHold, result.getSeatHold());
        assertEquals(List.of(heldSeat), result.getSeats());
        verify(saveSeatHoldPort, never()).save(any());
        verify(showtimeSeatRepositoryPort, never()).saveAll(anyList());
    }

    @Test
    void shouldRejectIdempotencyKeyReusedWithDifferentHash() {
        SeatHold existingHold = SeatHold.builder()
                .id(UUID.randomUUID())
                .bookingId(UUID.randomUUID())
                .showtimeId(UUID.randomUUID())
                .idempotencyKey("key-1")
                .requestHash("hash-1")
                .status(SeatHoldStatus.HELD)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        HoldSeatsCommand command = HoldSeatsCommand.builder()
                .showtimeId(UUID.randomUUID())
                .bookingId(UUID.randomUUID())
                .showtimeSeatIds(List.of(UUID.randomUUID()))
                .idempotencyKey("key-1")
                .requestHash("different-hash")
                .holdDurationSeconds(600)
                .build();

        when(loadSeatHoldPort.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existingHold));

        assertThrows(SeatHoldConflictException.class, () -> holdSeatsService.execute(command));

        verify(showtimeRepositoryPort, never()).findByIdWithLock(any());
        verify(saveSeatHoldPort, never()).save(any());
    }

    @Test
    void shouldRejectSeatsThatDoNotBelongToShowtime() {
        UUID requestedShowtimeId = UUID.randomUUID();
        UUID otherShowtimeId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();

        Showtime showtime = Showtime.builder()
                .id(requestedShowtimeId)
                .startTime(Instant.now().plusSeconds(3600))
                .status(ShowtimeStatus.SELLING)
                .build();
        ShowtimeSeat seat = ShowtimeSeat.builder()
                .id(seatId)
                .showtimeId(otherShowtimeId)
                .seatLabel("A1")
                .price(new BigDecimal("100000.00"))
                .status(ShowtimeSeatStatus.AVAILABLE)
                .build();
        HoldSeatsCommand command = HoldSeatsCommand.builder()
                .showtimeId(requestedShowtimeId)
                .bookingId(bookingId)
                .showtimeSeatIds(List.of(seatId))
                .idempotencyKey("key-1")
                .requestHash("hash-1")
                .holdDurationSeconds(600)
                .build();

        when(loadSeatHoldPort.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(loadSeatHoldPort.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(showtimeRepositoryPort.findByIdWithLock(requestedShowtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepositoryPort.findByIdsWithLock(anyList())).thenReturn(List.of(seat));

        assertThrows(SeatUnavailableException.class, () -> holdSeatsService.execute(command));

        verify(saveSeatHoldPort, never()).save(any());
        verify(showtimeSeatRepositoryPort, never()).saveAll(anyList());
    }
}
