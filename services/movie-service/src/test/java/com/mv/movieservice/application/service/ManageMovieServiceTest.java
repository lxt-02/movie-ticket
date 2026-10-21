package com.mv.movieservice.application.service;

import com.mv.movieservice.application.command.SaveMovieCommand;
import com.mv.movieservice.application.port.out.LoadMoviePort;
import com.mv.movieservice.application.port.out.SaveMoviePort;
import com.mv.movieservice.application.port.out.CataloguePort;
import com.mv.movieservice.application.port.out.MovieEventPort;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import com.mv.movieservice.domain.model.movie.exception.InvalidMovieException;
import com.mv.movieservice.domain.model.movie.exception.MovieNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageMovieServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");

    @Mock
    private LoadMoviePort loadMoviePort;
    @Mock
    private SaveMoviePort saveMoviePort;
    @Mock
    private CataloguePort cataloguePort;
    @Mock
    private MovieEventPort movieEventPort;

    private ManageMovieService service;

    @BeforeEach
    void setUp() {
        service = new ManageMovieService(loadMoviePort, saveMoviePort, cataloguePort, movieEventPort,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static SaveMovieCommand command(String title) {
        return SaveMovieCommand.builder().title(title).durationMinutes(120).ageRating("T16").build();
    }

    @Test
    void createSavesAComingSoonMovie() {
        when(saveMoviePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Movie movie = service.create(command("Avatar 3"));

        assertThat(movie.getStatus()).isEqualTo(MovieStatus.COMING_SOON);
        assertThat(movie.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void createWithoutDurationIsRejected() {
        SaveMovieCommand noDuration = SaveMovieCommand.builder().title("X").build();

        assertThatThrownBy(() -> service.create(noDuration)).isInstanceOf(InvalidMovieException.class);
        verify(saveMoviePort, never()).save(any());
    }

    @Test
    void updateUnknownMovieIs404() {
        UUID id = UUID.randomUUID();
        when(loadMoviePort.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, command("X"))).isInstanceOf(MovieNotFoundException.class);
    }

    @Test
    void archiveIsASoftDelete() {
        Movie existing = Movie.create(new com.mv.movieservice.domain.model.movie.valueobject.MovieDetails(
                "X", null, null, 90, null, null, null, null, null), null, NOW);
        when(loadMoviePort.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(saveMoviePort.save(existing)).thenReturn(existing);

        Movie archived = service.archive(existing.getId());

        assertThat(archived.getStatus()).isEqualTo(MovieStatus.ARCHIVED);
        verify(saveMoviePort).save(existing);
    }
}
