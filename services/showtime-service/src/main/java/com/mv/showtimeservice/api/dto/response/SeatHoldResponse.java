package com.mv.showtimeservice.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A seat hold. {@code status}: HELD, CONFIRMED, RELEASED or EXPIRED. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldResponse {
    private UUID holdId;
    private UUID bookingId;
    private UUID showtimeId;
    private String status;
    private Instant expiresAt;
    private String movieTitle;
    private String cinemaName;
    private String screenName;
    private Instant startTime;
    private List<HeldSeatResponse> seats;
}
