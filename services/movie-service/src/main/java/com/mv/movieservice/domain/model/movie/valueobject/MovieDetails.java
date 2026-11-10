package com.mv.movieservice.domain.model.movie.valueobject;

import com.mv.movieservice.domain.model.movie.exception.InvalidMovieException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

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

    /** Vietnamese film classification (P = all ages, K = under 13 with a parent, T13 / T16 / T18). */
    public static final List<String> AGE_RATINGS = List.of("P", "K", "T13", "T16", "T18");

    public MovieDetails {
        title = trimToNull(title);
        if (title == null) {
            throw new InvalidMovieException("Tên phim không được để trống");
        }
        if (title.length() > 255) {
            throw new InvalidMovieException("Tên phim tối đa 255 ký tự");
        }
        if (durationMinutes <= 0 || durationMinutes > MAX_DURATION_MINUTES) {
            throw new InvalidMovieException("Thời lượng phim phải từ 1 đến " + MAX_DURATION_MINUTES);
        }
        originalTitle = trimToNull(originalTitle);
        description = trimToNull(description);
        ageRating = trimToNull(ageRating);
        language = trimToNull(language);
        country = trimToNull(country);
        posterUrl = trimToNull(posterUrl);
        if (ageRating != null) {
            ageRating = ageRating.toUpperCase(Locale.ROOT);
            if (!AGE_RATINGS.contains(ageRating)) {
                throw new InvalidMovieException("Phân loại độ tuổi phải là một trong " + AGE_RATINGS);
            }
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
