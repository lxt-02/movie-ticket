package com.mv.movieservice.infrastructure.adapter;

import com.mv.movieservice.application.dto.PageResult;
import com.mv.movieservice.application.port.out.LoadMoviePort;
import com.mv.movieservice.application.port.out.SaveMoviePort;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import com.mv.movieservice.domain.repository.MovieRepository;
import com.mv.movieservice.infrastructure.persistence.entity.GenreEntity;
import com.mv.movieservice.infrastructure.persistence.entity.MovieCrewEntity;
import com.mv.movieservice.infrastructure.persistence.entity.MovieEntity;
import com.mv.movieservice.infrastructure.persistence.entity.PersonEntity;
import com.mv.movieservice.infrastructure.persistence.mapper.MoviePersistenceMapper;
import com.mv.movieservice.infrastructure.persistence.repository.GenreJpaRepository;
import com.mv.movieservice.infrastructure.persistence.repository.MovieCrewJpaRepository;
import com.mv.movieservice.infrastructure.persistence.repository.MovieJpaRepository;
import com.mv.movieservice.infrastructure.persistence.repository.PersonJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MovieRepositoryAdapter implements MovieRepository, LoadMoviePort, SaveMoviePort {

    private final MovieJpaRepository movieJpaRepository;
    private final GenreJpaRepository genreJpaRepository;
    private final MovieCrewJpaRepository movieCrewJpaRepository;
    private final PersonJpaRepository personJpaRepository;
    private final MoviePersistenceMapper mapper;

    @Override
    public Optional<Movie> findById(UUID id) {
        return movieJpaRepository.findById(id).map(entity -> toDomain(List.of(entity)).get(0));
    }

    @Override
    public PageResult<Movie> search(MovieStatus status, String keyword, boolean includeArchived, int page, int size) {
        Page<MovieEntity> result = movieJpaRepository.search(status, keyword, includeArchived, MovieStatus.ARCHIVED,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "releaseDate").and(Sort.by("title"))));
        return new PageResult<>(toDomain(result.getContent()), page, size, result.getTotalElements());
    }

    /**
     * Saves the movie row and its genres (movie_genres), then replaces its crew rows.
     * Returns the given movie: its genres and crew already carry the names resolved by the service.
     */
    @Override
    public Movie save(Movie movie) {
        MovieEntity entity = mapper.toEntity(movie);
        Set<GenreEntity> genres = new LinkedHashSet<>();
        movie.getGenres().forEach(g -> genres.add(genreJpaRepository.getReferenceById(g.id())));
        entity.setGenres(genres);
        movieJpaRepository.save(entity);

        movieCrewJpaRepository.deleteByMovieId(movie.getId());
        movieCrewJpaRepository.saveAll(mapper.toCrewEntities(movie));
        return movie;
    }

    /** Maps movies with their crew and person names in two extra queries for the whole page. */
    private List<Movie> toDomain(List<MovieEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Set<UUID> movieIds = entities.stream().map(MovieEntity::getId).collect(Collectors.toSet());
        Map<UUID, List<MovieCrewEntity>> crewByMovie = movieCrewJpaRepository.findByMovieIdIn(movieIds).stream()
                .collect(Collectors.groupingBy(MovieCrewEntity::getMovieId));
        Set<UUID> personIds = crewByMovie.values().stream()
                .flatMap(List::stream)
                .map(MovieCrewEntity::getPersonId)
                .collect(Collectors.toSet());
        Map<UUID, String> personNames = personIds.isEmpty() ? Map.of() : personJpaRepository.findAllById(personIds).stream()
                .collect(Collectors.toMap(PersonEntity::getId, PersonEntity::getName, (a, b) -> a));
        return entities.stream()
                .map(e -> mapper.toDomain(e, crewByMovie.getOrDefault(e.getId(), List.of()), personNames))
                .toList();
    }
}
