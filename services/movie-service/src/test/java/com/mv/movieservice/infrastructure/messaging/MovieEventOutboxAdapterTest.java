package com.mv.movieservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mv.movieservice.application.event.MovieEventType;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.enums.CrewRole;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import com.mv.movieservice.domain.model.movie.valueobject.CrewMember;
import com.mv.movieservice.domain.model.movie.valueobject.Genre;
import com.mv.movieservice.domain.model.movie.valueobject.MovieDetails;
import com.mv.movieservice.infrastructure.config.JsonCodec;
import com.mv.movieservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.mv.movieservice.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MovieEventOutboxAdapterTest {

    @Test
    void writesTheFullSnapshotWithUpdatedAtAsVersion() throws Exception {
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        Instant now = Instant.parse("2026-10-21T08:00:00Z");
        MovieEventOutboxAdapter adapter = new MovieEventOutboxAdapter(repository, new JsonCodec(), Clock.fixed(now, ZoneOffset.UTC));

        UUID directorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Genre drama = new Genre(UUID.randomUUID(), "Drama");
        Instant updatedAt = Instant.parse("2026-10-20T10:15:30Z");
        Movie movie = Movie.create(
                new MovieDetails("Lật Mặt 7", null, "Gia đình", 138, LocalDate.of(2024, 4, 26), "K", "Vietnamese", "Vietnam", null),
                List.of(drama),
                List.of(new CrewMember(directorId, "Lý Hải", CrewRole.DIRECTOR, null),
                        new CrewMember(actorId, "Thanh Hiền", CrewRole.ACTOR, "Bà Hai")),
                MovieStatus.NOW_SHOWING, updatedAt);

        adapter.publish(movie, MovieEventType.MOVIE_CREATED);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventEntity row = captor.getValue();
        assertThat(row.getAggregateId()).isEqualTo(movie.getId());
        assertThat(row.getEventType()).isEqualTo("MOVIE_CREATED");
        assertThat(row.getPublishedAt()).isNull();

        JsonNode message = new ObjectMapper().readTree(row.getPayload());
        assertThat(message.get("eventId").asText()).isEqualTo(row.getId().toString());
        assertThat(message.get("version").asLong()).isEqualTo(updatedAt.toEpochMilli());
        JsonNode payload = message.get("payload");
        assertThat(payload.get("title").asText()).isEqualTo("Lật Mặt 7");
        assertThat(payload.get("releaseDate").asText()).isEqualTo("2024-04-26");
        assertThat(payload.get("status").asText()).isEqualTo("NOW_SHOWING");
        assertThat(payload.get("genres").get(0).get("name").asText()).isEqualTo("Drama");
        assertThat(payload.get("directors").get(0).get("name").asText()).isEqualTo("Lý Hải");
        assertThat(payload.get("directors").get(0).has("characterName")).isFalse();
        assertThat(payload.get("actors").get(0).get("characterName").asText()).isEqualTo("Bà Hai");
    }
}
