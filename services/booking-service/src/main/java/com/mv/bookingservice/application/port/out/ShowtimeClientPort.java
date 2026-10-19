package com.mv.bookingservice.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ShowtimeClientPort {
    /**
     * Outbound contract to request seat holds from Showtime service.
     * Idempotent by bookingId: Showtime returns the existing hold when the same bookingId
     * is retried with the same seats. The client's Idempotency-Key is NOT forwarded.
     */
    SeatHoldResult holdSeats(UUID showtimeId, UUID bookingId, List<UUID> showtimeSeatIds);

    SeatHoldResult getHoldByBookingId(UUID bookingId);

    SeatHoldResult releaseHold(UUID holdId);

    /**
     * Turns the held seats into BOOKED.
     *
     * @throws com.mv.bookingservice.application.exception.SeatHoldRejectedException if Showtime refuses for good
     *         (hold released/expired, seats taken): do not retry, refund instead
     * @throws RuntimeException any other failure (timeout, 5xx) is temporary and may be retried
     */
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
