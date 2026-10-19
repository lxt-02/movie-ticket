package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.command.UpdateShowtimeCommand;
import com.mv.showtimeservice.application.dto.ShowtimeSchedulePolicy;
import com.mv.showtimeservice.application.port.out.ShowtimeRepositoryPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSeatRepositoryPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSnapshotPort;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeSeatStatus;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeStatus;
import com.mv.showtimeservice.domain.model.showtime.exception.InvalidShowtimeException;
import com.mv.showtimeservice.domain.model.showtime.exception.ShowtimeConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageShowtimeServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");

    @Mock
    private ShowtimeRepositoryPort showtimeRepositoryPort;
    @Mock
    private ShowtimeSeatRepositoryPort showtimeSeatRepositoryPort;
    @Mock
    private ShowtimeSnapshotPort showtimeSnapshotPort;

    private ManageShowtimeService service;
    private Showtime showtime;

    @BeforeEach
    void setUp() {
        service = new ManageShowtimeService(showtimeRepositoryPort, showtimeSeatRepositoryPort, showtimeSnapshotPort,
                new ShowtimeSchedulePolicy(Duration.ofMinutes(15)), Clock.fixed(NOW, ZoneOffset.UTC));
        Instant start = NOW.plus(Duration.ofDays(1));
        showtime = Showtime.builder()
                .id(UUID.randomUUID())
                .screenId(UUID.randomUUID())
                .startTime(start)
                .endTime(start.plus(Duration.ofMinutes(120)))
                .basePrice(new BigDecimal("100000.00"))
                .status(ShowtimeStatus.SCHEDULED)
                .build();
        // lenient: the empty-update test fails before the showtime is loaded
        lenient().when(showtimeRepositoryPort.findByIdWithLock(showtime.getId())).thenReturn(Optional.of(showtime));
    }

    private ShowtimeSeat seat(BigDecimal price) {
        return ShowtimeSeat.builder().id(UUID.randomUUID()).showtimeId(showtime.getId()).seatLabel("A1")
                .price(price).status(ShowtimeSeatStatus.AVAILABLE).build();
    }

    @Test
    void rescheduleKeepsTheLengthAndChecksTheScreen() {
        Instant newStart = NOW.plus(Duration.ofDays(2));
        when(showtimeRepositoryPort.save(showtime)).thenReturn(showtime);

        Showtime result = service.update(showtime.getId(), UpdateShowtimeCommand.builder().startTime(newStart).build());

        assertEquals(newStart, result.getStartTime());
        assertEquals(newStart.plus(Duration.ofMinutes(120)), result.getEndTime());
        verify(showtimeSnapshotPort).lockScreen(showtime.getScreenId());
        // the range is widened by the 15 min cleaning gap, and the showtime itself is excluded
        verify(showtimeRepositoryPort).existsOverlapping(eq(showtime.getScreenId()),
                eq(newStart.minus(Duration.ofMinutes(15))), eq(newStart.plus(Duration.ofMinutes(135))),
                eq(showtime.getId()));
    }

    @Test
    void overlappingScheduleIsRefused() {
        when(showtimeRepositoryPort.existsOverlapping(any(), any(), any(), any())).thenReturn(true);

        assertThrows(ShowtimeConflictException.class, () -> service.update(showtime.getId(),
                UpdateShowtimeCommand.builder().startTime(NOW.plus(Duration.ofDays(3))).build()));
        verify(showtimeRepositoryPort, never()).save(any());
    }

    @Test
    void priceChangeKeepsSeatMultipliers() {
        ShowtimeSeat normal = seat(new BigDecimal("100000.00"));
        ShowtimeSeat vip = seat(new BigDecimal("150000.00"));
        when(showtimeSeatRepositoryPort.findByShowtimeId(showtime.getId())).thenReturn(List.of(normal, vip));
        when(showtimeRepositoryPort.save(showtime)).thenReturn(showtime);

        service.update(showtime.getId(), UpdateShowtimeCommand.builder().basePrice(new BigDecimal("80000")).build());

        assertEquals(new BigDecimal("80000.00"), normal.getPrice());
        assertEquals(new BigDecimal("120000.00"), vip.getPrice());
        assertEquals(0, new BigDecimal("80000").compareTo(showtime.getBasePrice()));
    }

    @Test
    void showtimeWithSoldOrHeldSeatsCannotChangeOrBeCancelled() {
        when(showtimeSeatRepositoryPort.countTakenSeats(showtime.getId(), NOW)).thenReturn(2L);

        assertThrows(ShowtimeConflictException.class, () -> service.update(showtime.getId(),
                UpdateShowtimeCommand.builder().basePrice(BigDecimal.ONE).build()));
        assertThrows(ShowtimeConflictException.class, () -> service.cancel(showtime.getId()));
        verify(showtimeRepositoryPort, never()).save(any());
    }

    @Test
    void cancelBlocksEverySeat() {
        ShowtimeSeat a = seat(BigDecimal.TEN);
        ShowtimeSeat b = seat(BigDecimal.TEN);
        when(showtimeSeatRepositoryPort.findByShowtimeId(showtime.getId())).thenReturn(List.of(a, b));
        when(showtimeRepositoryPort.save(showtime)).thenReturn(showtime);

        Showtime result = service.cancel(showtime.getId());

        assertEquals(ShowtimeStatus.CANCELLED, result.getStatus());
        assertEquals(ShowtimeSeatStatus.BLOCKED, a.getStatus());
        assertEquals(ShowtimeSeatStatus.BLOCKED, b.getStatus());
    }

    @Test
    void startedShowtimeCannotBeEdited() {
        showtime.setStartTime(NOW.minusSeconds(60));

        assertThrows(InvalidShowtimeException.class, () -> service.update(showtime.getId(),
                UpdateShowtimeCommand.builder().basePrice(BigDecimal.ONE).build()));
    }

    @Test
    void emptyUpdateIsRejected() {
        assertThrows(InvalidShowtimeException.class,
                () -> service.update(showtime.getId(), UpdateShowtimeCommand.builder().build()));
    }
}
