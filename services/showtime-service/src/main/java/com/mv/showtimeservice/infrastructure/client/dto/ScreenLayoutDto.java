package com.mv.showtimeservice.infrastructure.client.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Screen + seats, answer of getScreenLayout. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenLayoutDto {
    private ScreenDto screen;
    private List<SeatDto> seats;
}
