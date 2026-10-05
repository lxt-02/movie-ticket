package com.mv.bookingservice.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    private UUID userId;

    @NotNull(message = "showtimeId must not be null")
    private UUID showtimeId;

    @NotBlank(message = "movieTitle must not be blank")
    private String movieTitle;

    @NotBlank(message = "cinemaName must not be blank")
    private String cinemaName;

    @NotBlank(message = "screenName must not be blank")
    private String screenName;

    @NotNull(message = "startTime must not be null")
    private Instant startTime;

    @NotNull(message = "holdId must not be null")
    private UUID holdId;

    @Builder.Default
    private String currency = "VND";

    private Instant expiresAt;

    @DecimalMin(value = "0.0", inclusive = true, message = "discountAmount must be greater than or equal to 0")
    private BigDecimal discountAmount;

    @NotEmpty(message = "seats list must not be empty")
    @Valid
    private List<CreateBookingSeatRequest> seats;
}
