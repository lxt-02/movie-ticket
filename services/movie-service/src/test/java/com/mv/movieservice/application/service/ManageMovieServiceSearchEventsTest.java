package com.mv.movieservice.application.service;

import com.mv.movieservice.application.command.SaveMovieCommand;
import com.mv.movieservice.application.event.MovieEventType;
import com.mv.movieservice.application.port.out.CataloguePort;
import com.mv.movieservice.application.port.out.LoadMoviePort;
import com.mv.movieservice.application.port.out.MovieEventPort;
import com.mv.movieservice.application.port.out.SaveMoviePort;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.enums.CrewRole;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import com.mv.movieservice.domain.model.movie.exception.InvalidMovieException;
import com.mv.movieservice.domain.model.movie.valueobject.Genre;
import com.mv.movieservice.domain.model.movie.valueobject.MovieDetails;
import com.mv.movieservice.domain.model.movie.valueobject.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageMovieServiceSearchEventsTest {

    private static final Instant NOW = Instant.parse("2026-10-21T08:00:00Z");
    private static final Genre ACTION = new Genre(UUID.randomUUID(), "Action");
    private static final Person DIRECTOR = new Person(UUID.randomUUID(), "Denis Villeneuve", null, null);
    private static final Person ACTOR = new Person(UUID.randomUUID(), "Zendaya", null, null);

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

    private static SaveMovieCommand.SaveMovieCommandBuilder command() {
        return SaveMovieCommand.builder().title("Dune: Part Two").durationMinutes(166).ageRating("t16");
    }

    @Test
    void createResolvesGenresAndCrewAndPublishesMovieCreated() {
        when(cataloguePort.findGenresByIds(any())).thenReturn(List.of(ACTION));
        when(cataloguePort.findPersonsByIds(any())).thenReturn(List.of(DIRECTOR, ACTOR));
        when(saveMoviePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Movie movie = service.create(command()
                .genreIds(List.of(ACTION.id()))
                .crew(List.of(
                        new SaveMovieCommand.CrewInput(DIRECTOR.id(), CrewRole.DIRECTOR, "ignored for directors"),
                        new SaveMovieCommand.CrewInput(ACTOR.id(), CrewRole.ACTOR, "Chani")))
                .build());

        assertThat(movie.getAgeRating()).isEqualTo("T16");
        assertThat(movie.getGenres()).containsExactly(ACTION);
        assertThat(movie.directors()).singleElement().satisfies(d -> {
            assertThat(d.name()).isEqualTo("Denis Villeneuve");
            assertThat(d.characterName()).isNull();
        });
        assertThat(movie.actors()).singleElement().satisfies(a -> assertThat(a.characterName()).isEqualTo("Chani"));
        verify(movieEventPort).publish(movie, MovieEventType.MOVIE_CREATED);
    }

    @Test
    void unknownGenreIsRejectedAndNothingIsPublished() {
        UUID unknown = UUID.randomUUID();
        when(cataloguePort.findGenresByIds(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(command().genreIds(List.of(unknown)).build()))
                .isInstanceOf(InvalidMovieException.class)
                .hasMessageContaining(unknown.toString());
        verify(saveMoviePort, never()).save(any());
        verify(movieEventPort, never()).publish(any(), any());
    }

    @Test
    void unknownPersonIsRejected() {
        when(cataloguePort.findPersonsByIds(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(command()
                .crew(List.of(new SaveMovieCommand.CrewInput(UUID.randomUUID(), CrewRole.ACTOR, null)))
                .build()))
                .isInstanceOf(InvalidMovieException.class);
    }

    @Test
    void samePersonTwiceInTheSameRoleIsRejected() {
        when(cataloguePort.findPersonsByIds(any())).thenReturn(List.of(ACTOR));

        assertThatThrownBy(() -> service.create(command()
                .crew(List.of(
                        new SaveMovieCommand.CrewInput(ACTOR.id(), CrewRole.ACTOR, "A"),
                        new SaveMovieCommand.CrewInput(ACTOR.id(), CrewRole.ACTOR, "B")))
                .build()))
                .isInstanceOf(InvalidMovieException.class);
    }

    @Test
    void invalidAgeRatingIsRejected() {
        assertThatThrownBy(() -> service.create(command().ageRating("R").build()))
                .isInstanceOf(InvalidMovieException.class)
                .hasMessageContaining("T18");
    }

    @Test
    void archivingPublishesMovieArchivedOnlyTheFirstTime() {
        Movie movie = Movie.create(new MovieDetails("Avatar", null, null, 120, null, null, null, null, null),
                MovieStatus.NOW_SHOWING, NOW.minusSeconds(60));
        when(loadMoviePort.findById(movie.getId())).thenReturn(Optional.of(movie));
        when(saveMoviePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.archive(movie.getId());
        service.archive(movie.getId());

        verify(movieEventPort).publish(movie, MovieEventType.MOVIE_ARCHIVED);
        verify(saveMoviePort).save(movie);
    }

    @Test
    void sameStatusChangeIsANoOp() {
        Movie movie = Movie.create(new MovieDetails("Avatar", null, null, 120, null, null, null, null, null),
                MovieStatus.NOW_SHOWING, NOW);
        when(loadMoviePort.findById(movie.getId())).thenReturn(Optional.of(movie));

        service.changeStatus(movie.getId(), MovieStatus.NOW_SHOWING);

        verify(saveMoviePort, never()).save(any());
        verify(movieEventPort, never()).publish(any(), any());
    }

    @Test
    void statusChangePublishesMovieStatusChanged() {
        Movie movie = Movie.create(new MovieDetails("Avatar", null, null, 120, null, null, null, null, null),
                MovieStatus.COMING_SOON, NOW);
        when(loadMoviePort.findById(movie.getId())).thenReturn(Optional.of(movie));
        when(saveMoviePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(movie.getId(), MovieStatus.NOW_SHOWING);

        verify(movieEventPort).publish(movie, MovieEventType.MOVIE_STATUS_CHANGED);
    }
}
