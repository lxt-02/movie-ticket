package com.mv.cinemaservice.domain.model.screen.entity;

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
public class SeatType {
    private UUID id;
    private String name;
    private BigDecimal priceMultiplier;
}
