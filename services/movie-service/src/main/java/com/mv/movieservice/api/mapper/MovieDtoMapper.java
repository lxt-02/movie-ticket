package com.mv.movieservice.api.mapper;

import com.mv.movieservice.api.dto.response.GenreResponse;
import com.mv.movieservice.api.dto.response.MovieResponse;
import com.mv.movieservice.api.dto.response.PersonResponse;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.valueobject.CrewMember;
import com.mv.movieservice.domain.model.movie.valueobject.Genre;
import com.mv.movieservice.domain.model.movie.valueobject.Person;
import org.springframework.stereotype.Component;

import java.util.List;

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
                .genres(movie.getGenres() == null ? List.of() : movie.getGenres().stream().map(this::toResponse).toList())
                .directors(people(movie.directors()))
                .cast(people(movie.actors()))
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();
    }

    public GenreResponse toResponse(Genre genre) {
        return new GenreResponse(genre.id(), genre.name());
    }

    public PersonResponse toResponse(Person person) {
        return new PersonResponse(person.id(), person.name(), person.avatarUrl(), person.biography());
    }

    private static List<MovieResponse.PersonRef> people(List<CrewMember> members) {
        return members.stream()
                .map(c -> new MovieResponse.PersonRef(c.personId(), c.name(), c.characterName()))
                .toList();
    }
}
