package com.mv.bookingservice.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
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
