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

    @NotNull(message = "Phim là bắt buộc")
    private UUID movieId;

    @NotNull(message = "Rạp là bắt buộc")
    private UUID cinemaId;

    @NotNull(message = "Phòng chiếu là bắt buộc")
    private UUID screenId;

    @NotNull(message = "Giờ chiếu là bắt buộc")
    @Future(message = "Giờ chiếu phải ở tương lai")
    private Instant startTime;

    @NotNull(message = "Giá vé là bắt buộc")
    @DecimalMin(value = "0.00", message = "Giá vé không được âm")
    private BigDecimal basePrice;
}
