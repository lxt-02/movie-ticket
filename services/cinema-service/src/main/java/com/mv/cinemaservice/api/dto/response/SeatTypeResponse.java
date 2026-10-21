package com.mv.cinemaservice.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SeatTypeResponse(UUID id, String name, BigDecimal priceMultiplier) {
}
