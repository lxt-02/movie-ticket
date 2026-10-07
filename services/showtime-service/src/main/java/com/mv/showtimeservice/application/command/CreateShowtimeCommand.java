package com.mv.showtimeservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShowtimeCommand {
    private UUID movieId;
    private UUID cinemaId;
    private UUID screenId;
    private Instant startTime;
    private BigDecimal basePrice;
}
