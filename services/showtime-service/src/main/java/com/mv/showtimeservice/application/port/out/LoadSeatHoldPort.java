package com.mv.showtimeservice.application.port.out;

import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;

import java.util.Optional;
import java.util.UUID;

public interface LoadSeatHoldPort {
    Optional<SeatHold> findById(UUID id);

    Optional<SeatHold> findByBookingId(UUID bookingId);

    /** Showtime of a hold, read without loading or locking the hold (used to take the showtime lock first). */
    Optional<UUID> findShowtimeIdByHoldId(UUID holdId);

    /** Hold row locked with SELECT ... FOR UPDATE until the transaction ends. */
    Optional<SeatHold> findByIdForUpdate(UUID id);
}
