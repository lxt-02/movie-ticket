package com.mv.showtimeservice.infrastructure.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ScreenLayoutResponse {
    private ScreenResponse screen;
    private List<SeatResponse> seats;
}
