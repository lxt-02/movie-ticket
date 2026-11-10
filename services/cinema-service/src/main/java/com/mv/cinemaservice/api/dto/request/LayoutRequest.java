package com.mv.cinemaservice.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** PUT /api/v1/screens/{id}/layout: the full new layout. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LayoutRequest {
    @NotEmpty(message = "Sơ đồ ghế phải có ít nhất một hàng")
    @Valid
    private List<SeatRowRequest> rows;
}
