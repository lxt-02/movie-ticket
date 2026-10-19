package com.mv.movieservice.domain.model.movie.valueobject;

import com.mv.movieservice.domain.model.movie.exception.InvalidMovieException;

import java.time.LocalDate;

/**
 * Editable information of a movie. Validated on construction, so a Movie never holds invalid details.
 */
public record MovieDetails(
        String title,
        String originalTitle,
        String description,
        int durationMinutes,
        LocalDate releaseDate,
        String ageRating,
        String language,
        String country,
        String posterUrl
) {
    public static final int MAX_DURATION_MINUTES = 600;

    public MovieDetails {
        title = trimToNull(title);
        if (title == null) {
            throw new InvalidMovieException("title must not be blank");
        }
        if (title.length() > 255) {
            throw new InvalidMovieException("title must be at most 255 characters");
        }
        if (durationMinutes <= 0 || durationMinutes > MAX_DURATION_MINUTES) {
            throw new InvalidMovieException("durationMinutes must be between 1 and " + MAX_DURATION_MINUTES);
        }
        originalTitle = trimToNull(originalTitle);
        description = trimToNull(description);
        ageRating = trimToNull(ageRating);
        language = trimToNull(language);
        country = trimToNull(country);
        posterUrl = trimToNull(posterUrl);
        if (ageRating != null && ageRating.length() > 20) {
            throw new InvalidMovieException("ageRating must be at most 20 characters");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
