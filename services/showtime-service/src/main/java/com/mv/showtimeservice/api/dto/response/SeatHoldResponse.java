package com.mv.showtimeservice.api.dto.response;

import com.mv.showtimeservice.domain.model.seathold.enums.SeatHoldStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldResponse {
    private UUID holdId;
    private UUID bookingId;
    private UUID showtimeId;
    private SeatHoldStatus status;
    private Instant expiresAt;
    private String movieTitle;
    private String cinemaName;
    private String screenName;
    private Instant startTime;
    private List<HeldSeatResponse> seats;
}
