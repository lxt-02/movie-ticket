package com.mv.movieservice.infrastructure.messaging;

import com.mv.movieservice.application.event.MovieEventType;
import com.mv.movieservice.application.port.out.MovieEventPort;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.valueobject.CrewMember;
import com.mv.movieservice.domain.model.movie.valueobject.Genre;
import com.mv.movieservice.infrastructure.config.JsonCodec;
import com.mv.movieservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.mv.movieservice.infrastructure.persistence.repository.OutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writes movie events into outbox_events; {@link MovieOutboxRelay} publishes them to Kafka.
 * <p>
 * Message on topic {@code movie-events}, key = movieId (one movie always lands on the same partition, so its
 * events stay in order):
 * <pre>
 * { eventId, eventType, aggregateId, occurredAt, version,
 *   payload: { id, title, originalTitle, description, durationMinutes, releaseDate, ageRating, language, country,
 *              posterUrl, status, genres: [{id, name}], directors: [{id, name}],
 *              actors: [{id, name, characterName}], createdAt, updatedAt } }
 * </pre>
 * {@code version} = updatedAt in epoch milliseconds: consumers use it to ignore an older snapshot that arrives
 * after a newer one (resync racing a change, replay from a dead-letter topic).
 */
@Component
@RequiredArgsConstructor
public class MovieEventOutboxAdapter implements MovieEventPort {

    private final OutboxEventJpaRepository repository;
    private final JsonCodec json;
    private final Clock clock;

    @Override
    public void publish(Movie movie, MovieEventType type) {
        UUID eventId = UUID.randomUUID();
        Instant now = clock.instant();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("eventId", eventId);
        message.put("eventType", type.name());
        message.put("aggregateId", movie.getId());
        message.put("occurredAt", now);
        message.put("version", version(movie));
        message.put("payload", snapshot(movie));
        repository.save(OutboxEventEntity.builder()
                .id(eventId)
                .aggregateId(movie.getId())
                .eventType(type.name())
                .payload(json.write(message))
                .createdAt(now)
                .build());
    }

    static long version(Movie movie) {
        Instant updatedAt = movie.getUpdatedAt() != null ? movie.getUpdatedAt() : movie.getCreatedAt();
        return updatedAt != null ? updatedAt.toEpochMilli() : 0L;
    }

    static Map<String, Object> snapshot(Movie movie) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", movie.getId());
        payload.put("title", movie.getTitle());
        payload.put("originalTitle", movie.getOriginalTitle());
        payload.put("description", movie.getDescription());
        payload.put("durationMinutes", movie.getDurationMinutes());
        payload.put("releaseDate", movie.getReleaseDate());
        payload.put("ageRating", movie.getAgeRating());
        payload.put("language", movie.getLanguage());
        payload.put("country", movie.getCountry());
        payload.put("posterUrl", movie.getPosterUrl());
        payload.put("status", movie.getStatus() != null ? movie.getStatus().name() : null);
        payload.put("genres", genres(movie.getGenres()));
        payload.put("directors", people(movie.directors(), false));
        payload.put("actors", people(movie.actors(), true));
        payload.put("createdAt", movie.getCreatedAt());
        payload.put("updatedAt", movie.getUpdatedAt());
        return payload;
    }

    private static List<Map<String, Object>> genres(List<Genre> genres) {
        return genres.stream().map(g -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.id());
            m.put("name", g.name());
            return m;
        }).toList();
    }

    private static List<Map<String, Object>> people(List<CrewMember> members, boolean withCharacter) {
        return members.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.personId());
            m.put("name", c.name());
            if (withCharacter) {
                m.put("characterName", c.characterName());
            }
            return m;
        }).toList();
    }
}
