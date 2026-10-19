package com.mv.showtimeservice.application.command;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/** Partial update: a null field keeps its current value. */
@Getter
@Builder
public class UpdateShowtimeCommand {
    private Instant startTime;
    private BigDecimal basePrice;
}
