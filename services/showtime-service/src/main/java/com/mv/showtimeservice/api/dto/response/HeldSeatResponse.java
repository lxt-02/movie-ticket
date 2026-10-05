package com.mv.showtimeservice.api.dto.response;

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
public class HeldSeatResponse {
    private UUID showtimeSeatId;
    private UUID seatId;
    private String seatLabel;
    private String seatType;
    private BigDecimal price;
}
