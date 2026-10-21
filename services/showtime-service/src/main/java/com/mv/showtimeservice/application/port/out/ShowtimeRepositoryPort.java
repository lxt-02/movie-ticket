package com.mv.showtimeservice.application.port.out;

import com.mv.showtimeservice.application.dto.PageResult;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeStatus;

import java.time.Instant;
import java.util.Collection;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;

import java.util.Optional;
import java.util.UUID;

public interface ShowtimeRepositoryPort {
    Optional<Showtime> findById(UUID id);

    Optional<Showtime> findByIdWithLock(UUID id);

    Showtime save(Showtime showtime);

    /**
     * True if another non-cancelled showtime of the screen overlaps [from, to).
     * The caller widens the range by the cleaning buffer.
     */
    boolean existsOverlapping(UUID screenId, Instant from, Instant to, UUID excludeShowtimeId);

    /** @return number of showtimes moved SCHEDULED -> SELLING */
    int openSales(Instant opensBefore, Instant now);

    /** @return number of showtimes moved to ENDED */
    int endFinished(Instant now);

    PageResult<Showtime> search(UUID movieId, UUID cinemaId, UUID screenId, Instant from, Instant to,
                                Collection<ShowtimeStatus> statuses, int page, int size);
}
