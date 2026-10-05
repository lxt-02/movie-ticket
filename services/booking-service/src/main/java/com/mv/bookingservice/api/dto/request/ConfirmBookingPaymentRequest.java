package com.mv.bookingservice.api.dto.request;

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
public class ConfirmBookingPaymentRequest {

    @NotNull(message = "paymentId must not be null")
    private UUID paymentId;

    @NotNull(message = "amount must not be null")
    private BigDecimal amount;

    @NotNull(message = "currency must not be null")
    private String currency;
}
