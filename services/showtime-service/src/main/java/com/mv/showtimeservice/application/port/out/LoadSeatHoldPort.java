package com.mv.showtimeservice.application.port.out;

import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;

import java.util.Optional;
import java.util.UUID;

public interface LoadSeatHoldPort {
    Optional<SeatHold> findById(UUID id);

    Optional<SeatHold> findByBookingId(UUID bookingId);

    Optional<SeatHold> findByIdempotencyKey(String idempotencyKey);
}
