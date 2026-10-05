package com.mv.movieservice.api.dto.response;

import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {
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
    private MovieStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
