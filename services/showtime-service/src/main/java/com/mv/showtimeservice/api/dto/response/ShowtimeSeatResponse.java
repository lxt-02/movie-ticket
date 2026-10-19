package com.mv.showtimeservice.api.dto.response;

import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeSeatStatus;

import java.math.BigDecimal;
import java.util.UUID;

/** One seat of the seat map. Use {@code showtimeSeatId} when creating a booking. */
public record ShowtimeSeatResponse(
        UUID showtimeSeatId,
        UUID seatId,
        String seatLabel,
        String seatType,
        BigDecimal price,
        ShowtimeSeatStatus status
) {
}
