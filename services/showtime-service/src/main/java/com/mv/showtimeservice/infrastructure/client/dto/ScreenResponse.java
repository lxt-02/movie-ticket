package com.mv.showtimeservice.infrastructure.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ScreenResponse {
    private UUID id;
    private UUID cinemaId;
    private String screenTypeName;
    private String name;
    private int totalSeats;
    private String status;
}
