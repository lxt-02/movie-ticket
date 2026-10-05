package com.mv.cinemaservice.domain.model.screen.entity;

import com.mv.cinemaservice.domain.model.screen.enums.SeatStatus;
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
public class Seat {
    private UUID id;
    private UUID screenId;
    private UUID seatTypeId;
    private String seatTypeName;
    private BigDecimal priceMultiplier;
    private String rowLabel;
    private int seatNumber;
    private String label;
    private SeatStatus status;

    public boolean isUsable() {
        return status == SeatStatus.ACTIVE;
    }
}
