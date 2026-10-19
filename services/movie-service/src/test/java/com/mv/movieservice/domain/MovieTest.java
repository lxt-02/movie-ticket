package com.mv.movieservice.domain;

import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import com.mv.movieservice.domain.model.movie.exception.InvalidMovieException;
import com.mv.movieservice.domain.model.movie.valueobject.MovieDetails;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovieTest {

    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");

    private static MovieDetails details(String title, int duration) {
        return new MovieDetails(title, null, "desc", duration, LocalDate.of(2026, 11, 1), "T13", "vi", "VN", null);
    }

    @Test
    void newMovieIsComingSoonByDefaultAndTrimsTitle() {
        Movie movie = Movie.create(details("  Dune 3  ", 150), null, NOW);

        assertThat(movie.getId()).isNotNull();
        assertThat(movie.getTitle()).isEqualTo("Dune 3");
        assertThat(movie.getStatus()).isEqualTo(MovieStatus.COMING_SOON);
        assertThat(movie.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void newMovieCannotStartArchivedOrEnded() {
        assertThatThrownBy(() -> Movie.create(details("X", 90), MovieStatus.ARCHIVED, NOW))
                .isInstanceOf(InvalidMovieException.class);
        assertThatThrownBy(() -> Movie.create(details("X", 90), MovieStatus.ENDED, NOW))
                .isInstanceOf(InvalidMovieException.class);
    }

    @Test
    void invalidDetailsAreRejected() {
        assertThatThrownBy(() -> details("  ", 90)).isInstanceOf(InvalidMovieException.class);
        assertThatThrownBy(() -> details("X", 0)).isInstanceOf(InvalidMovieException.class);
        assertThatThrownBy(() -> details("X", 601)).isInstanceOf(InvalidMovieException.class);
    }

    @Test
    void statusFollowsTheLifecycle() {
        Movie movie = Movie.create(details("X", 90), null, NOW);
        movie.changeStatus(MovieStatus.NOW_SHOWING, NOW);
        movie.changeStatus(MovieStatus.ENDED, NOW);
        movie.changeStatus(MovieStatus.NOW_SHOWING, NOW); // re-release
        assertThat(movie.getStatus()).isEqualTo(MovieStatus.NOW_SHOWING);

        assertThatThrownBy(() -> movie.changeStatus(MovieStatus.COMING_SOON, NOW))
                .isInstanceOf(InvalidMovieException.class);
    }

    @Test
    void archivedMovieIsFrozen() {
        Movie movie = Movie.create(details("X", 90), null, NOW);
        movie.archive(NOW);
        movie.archive(NOW); // idempotent

        assertThat(movie.isArchived()).isTrue();
        assertThatThrownBy(() -> movie.update(details("Y", 100), NOW)).isInstanceOf(InvalidMovieException.class);
        assertThatThrownBy(() -> movie.changeStatus(MovieStatus.NOW_SHOWING, NOW))
                .isInstanceOf(InvalidMovieException.class);
    }
}
