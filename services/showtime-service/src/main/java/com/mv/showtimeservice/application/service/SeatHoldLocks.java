package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.port.out.LoadSeatHoldPort;
import com.mv.showtimeservice.application.port.out.ShowtimeRepositoryPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSeatRepositoryPort;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;
import com.mv.showtimeservice.domain.model.seathold.entity.SeatHoldItem;
import com.mv.showtimeservice.domain.model.seathold.exception.SeatHoldNotFoundException;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Locks a hold and its seats for confirm / release, in the same order as {@link HoldSeatsService}:
 * showtime row, then the hold, then the seats in ascending id order.
 * <p>
 * Every writer of holds and seats first locks the showtime row, so for one showtime they run one at a time:
 * a confirm can no longer interleave with another buyer reclaiming the same expired seats (double sale), and the
 * shared order rules out deadlocks. Must be called inside a transaction; the locks last until it ends.
 */
@Component
@RequiredArgsConstructor
public class SeatHoldLocks {

    private final LoadSeatHoldPort loadSeatHoldPort;
    private final ShowtimeRepositoryPort showtimeRepositoryPort;
    private final ShowtimeSeatRepositoryPort showtimeSeatRepositoryPort;

    public record LockedHold(SeatHold hold, List<ShowtimeSeat> seats) {
    }

    public LockedHold lock(UUID holdId) {
        UUID showtimeId = loadSeatHoldPort.findShowtimeIdByHoldId(holdId)
                .orElseThrow(() -> new SeatHoldNotFoundException(holdId));
        showtimeRepositoryPort.findByIdWithLock(showtimeId);

        SeatHold hold = loadSeatHoldPort.findByIdForUpdate(holdId)
                .orElseThrow(() -> new SeatHoldNotFoundException(holdId));

        List<UUID> seatIds = hold.getItems() == null ? List.of() : hold.getItems().stream()
                .map(SeatHoldItem::getShowtimeSeatId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        List<ShowtimeSeat> seats = seatIds.isEmpty() ? List.of() : showtimeSeatRepositoryPort.findByIdsWithLock(seatIds);
        return new LockedHold(hold, seats);
    }
}
