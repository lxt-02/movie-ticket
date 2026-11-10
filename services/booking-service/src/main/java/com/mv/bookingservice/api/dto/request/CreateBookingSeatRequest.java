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

    @NotNull(message = "Ghế là bắt buộc")
    private UUID showtimeSeatId;

    private UUID seatId;

    private String seatLabel;

    @jakarta.validation.constraints.DecimalMin(value = "0.0", inclusive = true, message = "Giá ghế không được âm")
    private java.math.BigDecimal unitPrice;
}
