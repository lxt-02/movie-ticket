package com.mv.bookingservice.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateBookingSeatRequest {

    @NotNull(message = "showtimeSeatId must not be null")
    private UUID showtimeSeatId;

    @NotNull(message = "seatId must not be null")
    private UUID seatId;

    @NotBlank(message = "seatLabel must not be blank")
    private String seatLabel;

    @NotNull(message = "unitPrice must not be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "unitPrice must be greater than or equal to 0")
    private BigDecimal unitPrice;
}
