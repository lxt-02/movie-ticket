package com.mv.movieservice.infrastructure.adapter;

import com.mv.movieservice.application.command.SaveMovieCommand;
import com.mv.movieservice.application.port.in.ManageCatalogueUseCase;
import com.mv.movieservice.application.port.in.ManageMovieUseCase;
import com.mv.movieservice.application.port.out.LoadMoviePort;
import com.mv.movieservice.application.service.MovieSearchSyncService;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.enums.CrewRole;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import com.mv.movieservice.domain.model.movie.valueobject.Genre;
import com.mv.movieservice.domain.model.movie.valueobject.Person;
import com.mv.movieservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.mv.movieservice.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Real JPA mapping on H2: genres (movie_genres), crew (movie_crew) and outbox rows. */
@SpringBootTest
class MoviePersistenceIntegrationTest {

    @Autowired
    private ManageMovieUseCase manageMovieUseCase;
    @Autowired
    private ManageCatalogueUseCase manageCatalogueUseCase;
    @Autowired
    private MovieSearchSyncService movieSearchSyncService;
    @Autowired
    private LoadMoviePort loadMoviePort;
    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void savesGenresAndCrewReplacesThemOnUpdateAndWritesOutboxEvents() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Genre action = manageCatalogueUseCase.createGenre("Action " + suffix);
        Genre drama = manageCatalogueUseCase.createGenre("Drama " + suffix);
        Person director = manageCatalogueUseCase.createPerson("Denis Villeneuve " + suffix, null, null);
        Person actorA = manageCatalogueUseCase.createPerson("Zendaya " + suffix, null, null);
        Person actorB = manageCatalogueUseCase.createPerson("Timothée Chalamet " + suffix, null, null);

        Movie created = manageMovieUseCase.create(SaveMovieCommand.builder()
                .title("Dune " + suffix).durationMinutes(166).ageRating("T16")
                .genreIds(List.of(action.id()))
                .crew(List.of(
                        new SaveMovieCommand.CrewInput(director.id(), CrewRole.DIRECTOR, null),
                        new SaveMovieCommand.CrewInput(actorA.id(), CrewRole.ACTOR, "Chani")))
                .build());

        Movie loaded = transactionTemplate.execute(s -> loadMoviePort.findById(created.getId()).orElseThrow());
        assertThat(loaded.getGenres()).extracting(Genre::name).containsExactly(action.name());
        assertThat(loaded.directors()).singleElement().satisfies(d -> assertThat(d.name()).isEqualTo(director.name()));
        assertThat(loaded.actors()).singleElement().satisfies(a -> assertThat(a.characterName()).isEqualTo("Chani"));

        // PUT: other genres, same actor with another character, a new actor, no director.
        manageMovieUseCase.update(created.getId(), SaveMovieCommand.builder()
                .title("Dune: Part Two " + suffix).durationMinutes(166).ageRating("T16")
                .genreIds(List.of(action.id(), drama.id()))
                .crew(List.of(
                        new SaveMovieCommand.CrewInput(actorA.id(), CrewRole.ACTOR, "Chani (older)"),
                        new SaveMovieCommand.CrewInput(actorB.id(), CrewRole.ACTOR, "Paul Atreides")))
                .build());

        Movie updated = transactionTemplate.execute(s -> loadMoviePort.findById(created.getId()).orElseThrow());
        assertThat(updated.getTitle()).isEqualTo("Dune: Part Two " + suffix);
        assertThat(updated.getGenres()).extracting(Genre::id).containsExactlyInAnyOrder(action.id(), drama.id());
        assertThat(updated.directors()).isEmpty();
        assertThat(updated.actors()).hasSize(2)
                .anySatisfy(a -> assertThat(a.characterName()).isEqualTo("Chani (older)"))
                .anySatisfy(a -> assertThat(a.name()).isEqualTo(actorB.name()));

        manageMovieUseCase.changeStatus(created.getId(), MovieStatus.ARCHIVED);

        List<String> types = outboxEventJpaRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(created.getId()))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(OutboxEventEntity::getEventType)
                .toList();
        assertThat(types).containsExactly("MOVIE_CREATED", "MOVIE_UPDATED", "MOVIE_ARCHIVED");

        int queued = movieSearchSyncService.resyncAll();
        assertThat(queued).isGreaterThanOrEqualTo(1);
        assertThat(outboxEventJpaRepository.findAll()).anyMatch(e ->
                e.getAggregateId().equals(created.getId()) && e.getEventType().equals("MOVIE_SYNCED"));
    }
}
