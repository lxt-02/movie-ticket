package com.mv.showtimeservice.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** PUT /api/v1/showtimes/{id}: give startTime and/or basePrice; a missing field is not changed. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShowtimeRequest {

    @Future(message = "startTime must be in the future")
    private Instant startTime;

    @DecimalMin(value = "0.00", message = "basePrice must be greater than or equal to 0")
    private BigDecimal basePrice;
}
