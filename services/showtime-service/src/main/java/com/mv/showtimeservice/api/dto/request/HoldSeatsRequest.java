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

/** Body of holdSeats. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldSeatsRequest {

    @NotNull(message = "Mã booking là bắt buộc")
    private UUID bookingId;

    @NotEmpty(message = "Phải chọn ít nhất một ghế")
    private List<UUID> showtimeSeatIds;
}
