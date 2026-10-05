package com.mv.showtimeservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldSeatsCommand {
    private UUID showtimeId;
    private UUID bookingId;
    private List<UUID> showtimeSeatIds;
    private String idempotencyKey;
    private String requestHash;

    @Builder.Default
    private long holdDurationSeconds = 600; // 10 minutes default
}
