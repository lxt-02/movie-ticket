package com.mv.bookingservice.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One seat of a hold, with its price for this showtime. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeldSeatResponse {
    private UUID showtimeSeatId;
    private UUID seatId;
    private String seatLabel;
    private String seatType;
    private BigDecimal price;
}
