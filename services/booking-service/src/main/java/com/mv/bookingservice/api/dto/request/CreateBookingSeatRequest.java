package com.mv.bookingservice.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingSeatRequest {

    @NotNull(message = "showtimeSeatId must not be null")
    private UUID showtimeSeatId;

    private UUID seatId;

    private String seatLabel;

    @jakarta.validation.constraints.DecimalMin(value = "0.0", inclusive = true, message = "unitPrice must be greater than or equal to 0")
    private java.math.BigDecimal unitPrice;
}
