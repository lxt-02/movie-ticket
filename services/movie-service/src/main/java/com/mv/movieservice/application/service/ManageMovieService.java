package com.mv.movieservice.application.service;

import com.mv.movieservice.application.command.SaveMovieCommand;
import com.mv.movieservice.application.event.MovieEventType;
import com.mv.movieservice.application.port.in.ManageMovieUseCase;
import com.mv.movieservice.application.port.out.CataloguePort;
import com.mv.movieservice.application.port.out.LoadMoviePort;
import com.mv.movieservice.application.port.out.MovieEventPort;
import com.mv.movieservice.application.port.out.SaveMoviePort;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import com.mv.movieservice.domain.model.movie.exception.InvalidMovieException;
import com.mv.movieservice.domain.model.movie.exception.MovieNotFoundException;
import com.mv.movieservice.domain.model.movie.valueobject.CrewMember;
import com.mv.movieservice.domain.model.movie.valueobject.Genre;
import com.mv.movieservice.domain.model.movie.valueobject.MovieDetails;
import com.mv.movieservice.domain.model.movie.valueobject.Person;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin operations on movies. Every change is saved together with a movie event in the outbox (same transaction),
 * so search-service learns about it even if Kafka is down at that moment.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ManageMovieService implements ManageMovieUseCase {

    private final LoadMoviePort loadMoviePort;
    private final SaveMoviePort saveMoviePort;
    private final CataloguePort cataloguePort;
    private final MovieEventPort movieEventPort;
    private final Clock clock;

    @Override
    public Movie create(SaveMovieCommand command) {
        Movie movie = Movie.create(toDetails(command), resolveGenres(command), resolveCrew(command),
                command.getStatus(), now());
        Movie saved = saveMoviePort.save(movie);
        movieEventPort.publish(saved, MovieEventType.MOVIE_CREATED);
        return saved;
    }

    @Override
    public Movie update(UUID movieId, SaveMovieCommand command) {
        Movie movie = load(movieId);
        movie.update(toDetails(command), resolveGenres(command), resolveCrew(command), now());
        Movie saved = saveMoviePort.save(movie);
        movieEventPort.publish(saved, MovieEventType.MOVIE_UPDATED);
        return saved;
    }

    @Override
    public Movie changeStatus(UUID movieId, MovieStatus status) {
        Movie movie = load(movieId);
        if (!movie.changeStatus(status, now())) {
            return movie;
        }
        Movie saved = saveMoviePort.save(movie);
        movieEventPort.publish(saved, saved.isArchived() ? MovieEventType.MOVIE_ARCHIVED : MovieEventType.MOVIE_STATUS_CHANGED);
        return saved;
    }

    @Override
    public Movie archive(UUID movieId) {
        Movie movie = load(movieId);
        if (!movie.archive(now())) {
            return movie;
        }
        Movie saved = saveMoviePort.save(movie);
        movieEventPort.publish(saved, MovieEventType.MOVIE_ARCHIVED);
        return saved;
    }

    private Movie load(UUID movieId) {
        return loadMoviePort.findById(movieId).orElseThrow(() -> new MovieNotFoundException(movieId));
    }

    private Instant now() {
        return clock.instant();
    }

    private List<Genre> resolveGenres(SaveMovieCommand command) {
        if (command == null || command.getGenreIds() == null || command.getGenreIds().isEmpty()) {
            return List.of();
        }
        if (command.getGenreIds().stream().anyMatch(Objects::isNull)) {
            throw new InvalidMovieException("genreIds không được chứa giá trị rỗng");
        }
        Set<UUID> ids = new LinkedHashSet<>(command.getGenreIds());
        Map<UUID, Genre> found = cataloguePort.findGenresByIds(ids).stream()
                .collect(Collectors.toMap(Genre::id, Function.identity()));
        List<UUID> missing = ids.stream().filter(id -> !found.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidMovieException("Thể loại không tồn tại: " + missing);
        }
        return ids.stream().map(found::get).toList();
    }

    private List<CrewMember> resolveCrew(SaveMovieCommand command) {
        if (command == null || command.getCrew() == null || command.getCrew().isEmpty()) {
            return List.of();
        }
        if (command.getCrew().stream().anyMatch(c -> c == null || c.personId() == null)) {
            throw new InvalidMovieException("Mỗi thành viên đoàn phim cần personId");
        }
        Set<UUID> personIds = command.getCrew().stream()
                .map(SaveMovieCommand.CrewInput::personId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, Person> found = cataloguePort.findPersonsByIds(personIds).stream()
                .collect(Collectors.toMap(Person::id, Function.identity()));
        List<UUID> missing = personIds.stream().filter(id -> !found.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidMovieException("Người không tồn tại: " + missing);
        }
        return command.getCrew().stream()
                .map(c -> new CrewMember(c.personId(), found.get(c.personId()).name(), c.role(), c.characterName()))
                .toList();
    }

    private static MovieDetails toDetails(SaveMovieCommand c) {
        if (c == null) {
            throw new InvalidMovieException("Thiếu dữ liệu phim");
        }
        if (c.getDurationMinutes() == null) {
            throw new InvalidMovieException("Thời lượng phim là bắt buộc");
        }
        return new MovieDetails(c.getTitle(), c.getOriginalTitle(), c.getDescription(), c.getDurationMinutes(),
                c.getReleaseDate(), c.getAgeRating(), c.getLanguage(), c.getCountry(), c.getPosterUrl());
    }
}
