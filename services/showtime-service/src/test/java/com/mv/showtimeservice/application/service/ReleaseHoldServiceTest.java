package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.command.ReleaseHoldCommand;
import com.mv.showtimeservice.application.port.out.LoadSeatHoldPort;
import com.mv.showtimeservice.application.port.out.SaveSeatHoldPort;
import com.mv.showtimeservice.application.port.out.ShowtimeRepositoryPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSeatRepositoryPort;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;
import com.mv.showtimeservice.domain.model.seathold.entity.SeatHoldItem;
import com.mv.showtimeservice.domain.model.seathold.enums.SeatHoldStatus;
import com.mv.showtimeservice.domain.model.seathold.exception.SeatHoldConflictException;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeSeatStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseHoldServiceTest {

    @Mock
    private LoadSeatHoldPort loadSeatHoldPort;
    @Mock
    private SaveSeatHoldPort saveSeatHoldPort;
    @Mock
    private ShowtimeSeatRepositoryPort showtimeSeatRepositoryPort;
    @Mock
    private ShowtimeRepositoryPort showtimeRepositoryPort;

    @Test
    void releasesOnlySeatsStillOwnedByTheHold() {
        UUID ownSeatId = UUID.randomUUID();
        UUID reclaimedSeatId = UUID.randomUUID();
        SeatHold hold = SeatHold.builder()
                .id(UUID.randomUUID())
                .showtimeId(UUID.randomUUID())
                .status(SeatHoldStatus.HELD)
                .expiresAt(Instant.now().plusSeconds(300))
                .items(new ArrayList<>(List.of(
                        SeatHoldItem.builder().showtimeSeatId(ownSeatId).build(),
                        SeatHoldItem.builder().showtimeSeatId(reclaimedSeatId).build())))
                .build();
        UUID otherHoldId = UUID.randomUUID();
        ShowtimeSeat own = ShowtimeSeat.builder().id(ownSeatId).status(ShowtimeSeatStatus.HELD).holdId(hold.getId()).build();
        ShowtimeSeat reclaimed = ShowtimeSeat.builder().id(reclaimedSeatId).status(ShowtimeSeatStatus.HELD).holdId(otherHoldId).build();

        when(loadSeatHoldPort.findShowtimeIdByHoldId(hold.getId())).thenReturn(Optional.of(hold.getShowtimeId()));
        when(loadSeatHoldPort.findByIdForUpdate(hold.getId())).thenReturn(Optional.of(hold));
        when(showtimeSeatRepositoryPort.findByIdsWithLock(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(own, reclaimed));
        when(saveSeatHoldPort.save(hold)).thenReturn(hold);

        SeatHoldLocks locks = new SeatHoldLocks(loadSeatHoldPort, showtimeRepositoryPort, showtimeSeatRepositoryPort);
        new ReleaseHoldService(locks, saveSeatHoldPort, showtimeSeatRepositoryPort)
                .execute(ReleaseHoldCommand.builder().holdId(hold.getId()).build());

        assertEquals(SeatHoldStatus.RELEASED, hold.getStatus());
        assertEquals(ShowtimeSeatStatus.AVAILABLE, own.getStatus());
        assertNull(own.getHoldId());
        assertEquals(ShowtimeSeatStatus.HELD, reclaimed.getStatus());
        assertEquals(otherHoldId, reclaimed.getHoldId());
        verify(showtimeSeatRepositoryPort).saveAll(List.of(own));
        verify(showtimeRepositoryPort).findByIdWithLock(hold.getShowtimeId());
    }

    private SeatHold stubHold(SeatHoldStatus status) {
        SeatHold hold = SeatHold.builder()
                .id(UUID.randomUUID())
                .showtimeId(UUID.randomUUID())
                .status(status)
                .expiresAt(Instant.now().minusSeconds(60))
                .items(new ArrayList<>())
                .build();
        when(loadSeatHoldPort.findShowtimeIdByHoldId(hold.getId())).thenReturn(Optional.of(hold.getShowtimeId()));
        when(loadSeatHoldPort.findByIdForUpdate(hold.getId())).thenReturn(Optional.of(hold));
        return hold;
    }

    private ReleaseHoldService service() {
        return new ReleaseHoldService(
                new SeatHoldLocks(loadSeatHoldPort, showtimeRepositoryPort, showtimeSeatRepositoryPort),
                saveSeatHoldPort, showtimeSeatRepositoryPort);
    }

    @Test
    void releasingAnExpiredHoldSucceedsSoBookingCancelNeverFails() {
        SeatHold hold = stubHold(SeatHoldStatus.EXPIRED);

        SeatHold result = service().execute(ReleaseHoldCommand.builder().holdId(hold.getId()).build());

        assertEquals(SeatHoldStatus.EXPIRED, result.getStatus());
        verify(saveSeatHoldPort, never()).save(any());
    }

    @Test
    void confirmedHoldIsNeverReleased() {
        SeatHold hold = stubHold(SeatHoldStatus.CONFIRMED);

        assertThrows(SeatHoldConflictException.class,
                () -> service().execute(ReleaseHoldCommand.builder().holdId(hold.getId()).build()));
        verify(showtimeSeatRepositoryPort, never()).saveAll(any());
    }
}
