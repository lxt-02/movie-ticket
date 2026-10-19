package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.command.ConfirmHoldCommand;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmHoldServiceTest {

    @Mock
    private LoadSeatHoldPort loadSeatHoldPort;
    @Mock
    private SaveSeatHoldPort saveSeatHoldPort;
    @Mock
    private ShowtimeSeatRepositoryPort showtimeSeatRepositoryPort;
    @Mock
    private ShowtimeRepositoryPort showtimeRepositoryPort;

    private ConfirmHoldService service;
    private SeatHold hold;
    private UUID seatId;

    @BeforeEach
    void setUp() {
        SeatHoldLocks locks = new SeatHoldLocks(loadSeatHoldPort, showtimeRepositoryPort, showtimeSeatRepositoryPort);
        service = new ConfirmHoldService(locks, saveSeatHoldPort, showtimeSeatRepositoryPort);
        seatId = UUID.randomUUID();
        hold = SeatHold.builder()
                .id(UUID.randomUUID())
                .bookingId(UUID.randomUUID())
                .showtimeId(UUID.randomUUID())
                .status(SeatHoldStatus.HELD)
                .expiresAt(Instant.now().plusSeconds(300))
                .items(new ArrayList<>(List.of(SeatHoldItem.builder().showtimeSeatId(seatId).build())))
                .build();
        when(loadSeatHoldPort.findShowtimeIdByHoldId(hold.getId())).thenReturn(Optional.of(hold.getShowtimeId()));
        when(loadSeatHoldPort.findByIdForUpdate(hold.getId())).thenReturn(Optional.of(hold));
    }

    private ShowtimeSeat seat(UUID holdId, ShowtimeSeatStatus status) {
        return ShowtimeSeat.builder()
                .id(seatId)
                .showtimeId(hold.getShowtimeId())
                .seatLabel("A1")
                .price(new BigDecimal("100000"))
                .status(status)
                .holdId(holdId)
                .build();
    }

    private ConfirmHoldCommand command() {
        return ConfirmHoldCommand.builder().holdId(hold.getId()).build();
    }

    @Test
    void confirmsHeldSeatsAfterLockingShowtimeThenHoldThenSeats() {
        ShowtimeSeat seat = seat(hold.getId(), ShowtimeSeatStatus.HELD);
        when(showtimeSeatRepositoryPort.findByIdsWithLock(List.of(seatId))).thenReturn(List.of(seat));
        when(saveSeatHoldPort.save(hold)).thenReturn(hold);

        SeatHold result = service.execute(command());

        assertEquals(SeatHoldStatus.CONFIRMED, result.getStatus());
        assertEquals(ShowtimeSeatStatus.BOOKED, seat.getStatus());
        InOrder order = inOrder(showtimeRepositoryPort, loadSeatHoldPort, showtimeSeatRepositoryPort);
        order.verify(showtimeRepositoryPort).findByIdWithLock(hold.getShowtimeId());
        order.verify(loadSeatHoldPort).findByIdForUpdate(hold.getId());
        order.verify(showtimeSeatRepositoryPort).findByIdsWithLock(List.of(seatId));
    }

    @Test
    void alreadyConfirmedIsIdempotent() {
        hold.setStatus(SeatHoldStatus.CONFIRMED);

        assertEquals(SeatHoldStatus.CONFIRMED, service.execute(command()).getStatus());
        verify(saveSeatHoldPort, never()).save(any());
    }

    @Test
    void expiredHoldIsRejectedNotReportedAsSuccess() {
        hold.setExpiresAt(Instant.now().minusSeconds(1));

        assertThrows(SeatHoldConflictException.class, () -> service.execute(command()));
        verify(saveSeatHoldPort, never()).save(any());
        verify(showtimeSeatRepositoryPort, never()).saveAll(any());
    }

    @Test
    void releasedOrExpiredHoldIsRejected() {
        hold.setStatus(SeatHoldStatus.EXPIRED);
        assertThrows(SeatHoldConflictException.class, () -> service.execute(command()));

        hold.setStatus(SeatHoldStatus.RELEASED);
        assertThrows(SeatHoldConflictException.class, () -> service.execute(command()));
        verify(saveSeatHoldPort, never()).save(any());
    }

    @Test
    void seatTakenByAnotherHoldIsRejected() {
        ShowtimeSeat seat = seat(UUID.randomUUID(), ShowtimeSeatStatus.HELD);
        when(showtimeSeatRepositoryPort.findByIdsWithLock(List.of(seatId))).thenReturn(List.of(seat));

        assertThrows(SeatHoldConflictException.class, () -> service.execute(command()));
        verify(showtimeSeatRepositoryPort, never()).saveAll(any());
    }
}
