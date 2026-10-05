package com.mv.bookingservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingCommand {
    private UUID userId;
    private UUID showtimeId;
    private String movieTitle;
    private String cinemaName;
    private String screenName;
    private Instant startTime;
    private UUID holdId;
    private String idempotencyKey;
    private String requestHash;
    private String currency;
    private Instant expiresAt;
    private BigDecimal discountAmount;
    private List<CreateBookingSeatCommand> seats;
}
