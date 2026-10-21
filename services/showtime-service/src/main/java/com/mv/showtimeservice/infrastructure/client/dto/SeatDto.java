package com.mv.showtimeservice.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A physical seat. status: ACTIVE / BROKEN. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatDto {
    private UUID id;
    private UUID screenId;
    private UUID seatTypeId;
    private String seatTypeName;
    private BigDecimal priceMultiplier;
    private String rowLabel;
    private int seatNumber;
    private String label;
    private String status;
}
