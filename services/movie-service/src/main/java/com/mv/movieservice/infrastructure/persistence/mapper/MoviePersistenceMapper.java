package com.mv.movieservice.infrastructure.persistence.mapper;

import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.valueobject.CrewMember;
import com.mv.movieservice.domain.model.movie.valueobject.Genre;
import com.mv.movieservice.domain.model.movie.valueobject.Person;
import com.mv.movieservice.infrastructure.persistence.entity.GenreEntity;
import com.mv.movieservice.infrastructure.persistence.entity.MovieCrewEntity;
import com.mv.movieservice.infrastructure.persistence.entity.MovieEntity;
import com.mv.movieservice.infrastructure.persistence.entity.PersonEntity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class MoviePersistenceMapper {

    /**
     * @param crew        crew rows of this movie
     * @param personNames person id -> name, for the crew rows
     */
    public Movie toDomain(MovieEntity entity, List<MovieCrewEntity> crew, Map<UUID, String> personNames) {
        if (entity == null) {
            return null;
        }
        List<Genre> genres = entity.getGenres() == null ? List.of() : entity.getGenres().stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(Genre::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<CrewMember> members = crew == null ? List.of() : crew.stream()
                .map(c -> new CrewMember(c.getPersonId(), personNames.get(c.getPersonId()), c.getRole(), c.getCharacterName()))
                .sorted(Comparator.comparing(CrewMember::role)
                        .thenComparing(m -> m.name() == null ? "" : m.name(), String.CASE_INSENSITIVE_ORDER))
                .toList();
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
                .genres(new java.util.ArrayList<>(genres))
                .crew(new java.util.ArrayList<>(members))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /** Movie row only; genres are attached by the adapter (as references), crew is saved separately. */
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

    public List<MovieCrewEntity> toCrewEntities(Movie domain) {
        return domain.getCrew().stream()
                .map(c -> MovieCrewEntity.builder()
                        .movieId(domain.getId())
                        .personId(c.personId())
                        .role(c.role())
                        .characterName(c.characterName())
                        .build())
                .toList();
    }

    public Genre toDomain(GenreEntity entity) {
        return new Genre(entity.getId(), entity.getName());
    }

    public Person toDomain(PersonEntity entity) {
        return new Person(entity.getId(), entity.getName(), entity.getAvatarUrl(), entity.getBiography());
    }

    public PersonEntity toEntity(Person person) {
        return PersonEntity.builder()
                .id(person.id())
                .name(person.name())
                .avatarUrl(person.avatarUrl())
                .biography(person.biography())
                .build();
    }
}
