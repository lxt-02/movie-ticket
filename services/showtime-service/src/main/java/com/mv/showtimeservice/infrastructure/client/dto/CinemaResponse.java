package com.mv.showtimeservice.infrastructure.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CinemaResponse {
    private UUID id;
    private String name;
    private String status;
}
