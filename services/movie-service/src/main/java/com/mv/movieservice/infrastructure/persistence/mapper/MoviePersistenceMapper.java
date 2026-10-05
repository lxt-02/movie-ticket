package com.mv.movieservice.infrastructure.persistence.mapper;

import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.infrastructure.persistence.entity.MovieEntity;
import org.springframework.stereotype.Component;

@Component
public class MoviePersistenceMapper {

    public Movie toDomain(MovieEntity entity) {
        if (entity == null) {
            return null;
        }
        return Movie.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .originalTitle(entity.getOriginalTitle())
                .description(entity.getDescription())
                .durationMinutes(entity.getDurationMinutes())
                .releaseDate(entity.getReleaseDate())
                .ageRating(entity.getAgeRating())
                .language(entity.getLanguage())
                .country(entity.getCountry())
                .posterUrl(entity.getPosterUrl())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public MovieEntity toEntity(Movie domain) {
        if (domain == null) {
            return null;
        }
        return MovieEntity.builder()
                .id(domain.getId())
                .title(domain.getTitle())
                .originalTitle(domain.getOriginalTitle())
                .description(domain.getDescription())
                .durationMinutes(domain.getDurationMinutes())
                .releaseDate(domain.getReleaseDate())
                .ageRating(domain.getAgeRating())
                .language(domain.getLanguage())
                .country(domain.getCountry())
                .posterUrl(domain.getPosterUrl())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
