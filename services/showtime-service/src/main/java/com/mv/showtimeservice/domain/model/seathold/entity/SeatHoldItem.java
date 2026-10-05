package com.mv.showtimeservice.domain.model.seathold.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldItem {
    private UUID holdId;
    private UUID showtimeId;
    private UUID showtimeSeatId;
    private BigDecimal unitPrice;
}
