package com.mv.bookingservice.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private String movieTitle;

    private String cinemaName;

    private String screenName;

    private java.time.Instant startTime;

    private UUID holdId;

    @Builder.Default
    private String currency = "VND";

    private java.time.Instant expiresAt;

    @jakarta.validation.constraints.DecimalMin(value = "0.0", inclusive = true, message = "discountAmount must be greater than or equal to 0")
    private java.math.BigDecimal discountAmount;

    @NotEmpty(message = "seats list must not be empty")
    @Valid
    private List<CreateBookingSeatRequest> seats;
}
