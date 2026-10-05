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
}
