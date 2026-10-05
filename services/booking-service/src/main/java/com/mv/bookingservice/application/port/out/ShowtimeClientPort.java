package com.mv.bookingservice.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ShowtimeClientPort {
    /**
     * Outbound contract to request seat holds from Showtime service.
     * OpenFeign or HTTP/Saga implementation to be attached later.
     */
    SeatHoldResult holdSeats(UUID showtimeId, UUID bookingId, List<UUID> showtimeSeatIds, String idempotencyKey);

    SeatHoldResult getHoldByBookingId(UUID bookingId);

    SeatHoldResult releaseHold(UUID holdId);

    SeatHoldResult confirmHold(UUID holdId);

    record SeatHoldResult(
            UUID holdId,
            Instant expiresAt,
            String movieTitle,
            String cinemaName,
            String screenName,
            Instant startTime,
            List<HeldSeatDetail> seats
    ) {}

    record HeldSeatDetail(
            UUID showtimeSeatId,
            UUID seatId,
            String seatLabel,
            java.math.BigDecimal price
    ) {}
}
