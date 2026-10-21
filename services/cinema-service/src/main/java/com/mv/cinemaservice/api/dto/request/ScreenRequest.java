package com.mv.cinemaservice.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * POST /api/v1/cinemas/{cinemaId}/screens (rows required) and PUT /api/v1/screens/{id} (rows ignored).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenRequest {

    @NotBlank(message = "Tên là bắt buộc")
    @Size(max = 100, message = "Tên phòng tối đa 100 ký tự")
    private String name;

    @NotNull(message = "Loại phòng là bắt buộc")
    private UUID screenTypeId;

    @Valid
    private List<SeatRowRequest> rows;
}
