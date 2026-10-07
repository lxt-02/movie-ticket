package com.mv.showtimeservice.infrastructure.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class MovieResponse {
    private UUID id;
    private String title;
    private int durationMinutes;
    private String ageRating;
    private String status;
}
