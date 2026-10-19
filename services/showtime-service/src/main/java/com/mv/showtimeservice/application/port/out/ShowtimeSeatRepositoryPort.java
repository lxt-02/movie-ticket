package com.mv.showtimeservice.application.port.out;

import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ShowtimeSeatRepositoryPort {
    List<ShowtimeSeat> findByIdsWithLock(List<UUID> ids);

    List<ShowtimeSeat> findByHoldId(UUID holdId);

    List<ShowtimeSeat> saveAll(List<ShowtimeSeat> seats);

    boolean existsByShowtimeId(UUID showtimeId);

    List<ShowtimeSeat> findByShowtimeId(UUID showtimeId);

    /** Seats BOOKED, or HELD with a hold that has not expired at {@code now}. */
    long countTakenSeats(UUID showtimeId, Instant now);
}
