package com.mv.showtimeservice.api.dto.request;

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
public class HoldSeatsRequest {

    @NotNull(message = "bookingId must not be null")
    private UUID bookingId;

    @NotEmpty(message = "showtimeSeatIds must not be empty")
    private List<UUID> showtimeSeatIds;
}
