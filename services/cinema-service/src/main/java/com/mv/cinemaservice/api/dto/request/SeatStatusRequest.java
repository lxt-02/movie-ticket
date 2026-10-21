package com.mv.cinemaservice.api.dto.request;

import com.mv.cinemaservice.domain.model.screen.enums.SeatStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusRequest {
    @NotNull(message = "Trạng thái là bắt buộc")
    private SeatStatus status;
}
