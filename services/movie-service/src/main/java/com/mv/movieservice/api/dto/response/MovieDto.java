package com.mv.movieservice.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A movie. status: COMING_SOON / NOW_SHOWING / ENDED / ARCHIVED. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieDto {
    private UUID id;
    private String title;
    private String originalTitle;
    private String description;
    private int durationMinutes;
    private LocalDate releaseDate;
    private String ageRating;
    private String language;
    private String country;
    private String posterUrl;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
