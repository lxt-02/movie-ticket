package com.mv.movieservice.api.mapper;

import com.mv.movieservice.api.dto.response.MovieResponse;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import org.springframework.stereotype.Component;

@Component
public class MovieDtoMapper {

    public MovieResponse toResponse(Movie movie) {
        if (movie == null) {
            return null;
        }

        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .originalTitle(movie.getOriginalTitle())
                .description(movie.getDescription())
                .durationMinutes(movie.getDurationMinutes())
                .releaseDate(movie.getReleaseDate())
                .ageRating(movie.getAgeRating())
                .language(movie.getLanguage())
                .country(movie.getCountry())
                .posterUrl(movie.getPosterUrl())
                .status(movie.getStatus())
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();
    }
}
