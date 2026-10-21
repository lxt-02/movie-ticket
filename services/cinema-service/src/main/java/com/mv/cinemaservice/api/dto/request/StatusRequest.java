package com.mv.cinemaservice.api.dto.request;

import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusRequest {
    @NotNull(message = "Trạng thái là bắt buộc")
    private CinemaStatus status;
}
