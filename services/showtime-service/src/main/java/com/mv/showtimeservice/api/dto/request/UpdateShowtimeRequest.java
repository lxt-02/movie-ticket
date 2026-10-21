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

    @Future(message = "Giờ chiếu phải ở tương lai")
    private Instant startTime;

    @DecimalMin(value = "0.00", message = "Giá vé không được âm")
    private BigDecimal basePrice;
}
