package com.mv.showtimeservice.infrastructure.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class SeatResponse {
    private UUID id;
    private String seatTypeName;
    private BigDecimal priceMultiplier;
    private String label;
    private String status;
}
