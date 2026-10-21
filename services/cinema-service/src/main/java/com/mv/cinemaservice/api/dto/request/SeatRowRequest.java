package com.mv.cinemaservice.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** { "rowLabel": "A", "seatCount": 12, "seatTypeId": "..." } -> seats A1..A12. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatRowRequest {

    @NotBlank(message = "Tên hàng ghế là bắt buộc")
    private String rowLabel;

    @NotNull(message = "Số ghế của hàng là bắt buộc")
    @Min(value = 1, message = "Mỗi hàng phải có ít nhất 1 ghế")
    @Max(value = 50, message = "Mỗi hàng tối đa 50 ghế")
    private Integer seatCount;

    @NotNull(message = "Loại ghế là bắt buộc")
    private UUID seatTypeId;
}
