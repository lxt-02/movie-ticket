package com.mv.showtimeservice.domain.repository;

import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;

import java.util.Optional;
import java.util.UUID;

public interface SeatHoldRepository {
    SeatHold save(SeatHold hold);
    Optional<SeatHold> findById(UUID id);
    Optional<SeatHold> findByBookingId(UUID bookingId);
    Optional<SeatHold> findByIdempotencyKey(String idempotencyKey);
}
