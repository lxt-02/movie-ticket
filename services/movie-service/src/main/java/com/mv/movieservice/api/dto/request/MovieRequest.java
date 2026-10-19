package com.mv.movieservice.api.dto.request;

import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Body of POST /api/v1/movies and PUT /api/v1/movies/{id} (PUT replaces every field). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be at most 255 characters")
    private String title;

    @Size(max = 255, message = "originalTitle must be at most 255 characters")
    private String originalTitle;

    private String description;

    @NotNull(message = "durationMinutes is required")
    @Min(value = 1, message = "durationMinutes must be at least 1")
    @Max(value = 600, message = "durationMinutes must be at most 600")
    private Integer durationMinutes;

    private LocalDate releaseDate;

    @Size(max = 20, message = "ageRating must be at most 20 characters")
    private String ageRating;

    @Size(max = 50, message = "language must be at most 50 characters")
    private String language;

    @Size(max = 100, message = "country must be at most 100 characters")
    private String country;

    private String posterUrl;

    /** Create only: COMING_SOON (default) or NOW_SHOWING. Ignored on update; use PATCH /status. */
    private MovieStatus status;
}
