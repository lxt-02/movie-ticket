package com.mv.showtimeservice.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShowtimeRequest {

    @NotNull(message = "movieId must not be null")
    private UUID movieId;

    @NotNull(message = "cinemaId must not be null")
    private UUID cinemaId;

    @NotNull(message = "screenId must not be null")
    private UUID screenId;

    @NotNull(message = "startTime must not be null")
    @Future(message = "startTime must be in the future")
    private Instant startTime;

    @NotNull(message = "basePrice must not be null")
    @DecimalMin(value = "0.00", message = "basePrice must be greater than or equal to 0")
    private BigDecimal basePrice;
}
