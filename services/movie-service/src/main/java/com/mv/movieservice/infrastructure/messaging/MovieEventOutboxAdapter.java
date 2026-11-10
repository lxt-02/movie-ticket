package com.mv.movieservice.infrastructure.messaging;

import com.mv.movieservice.application.event.MovieEventType;
import com.mv.movieservice.application.port.out.MovieEventPort;
import com.mv.movieservice.application.port.out.OutboxMessagePort;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.valueobject.CrewMember;
import com.mv.movieservice.domain.model.movie.valueobject.Genre;
import com.mv.movieservice.domain.model.outbox.OutboxMessage;
import com.mv.movieservice.infrastructure.config.JsonCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ghi event phim vào bảng outbox_messages, cùng transaction với thay đổi phim; {@link MovieOutboxRelay} mới là bên
 * gửi lên Kafka.
 * <p>
 * Message trên topic {@code movie-events}, key = movieId (một phim luôn vào cùng partition nên event giữ thứ tự):
 * <pre>
 * { eventId, eventType, aggregateId, occurredAt, version,
 *   payload: { id, title, originalTitle, description, durationMinutes, releaseDate, ageRating, language, country,
 *              posterUrl, status, genres: [{id, name}], directors: [{id, name}],
 *              actors: [{id, name, characterName}], createdAt, updatedAt } }
 * </pre>
 * {@code version} = updatedAt tính bằng epoch milli: consumer dùng nó để bỏ qua bản cũ tới sau bản mới (đồng bộ lại
 * chạy song song với một thay đổi, phát lại từ DLT).
 */
@Component
@RequiredArgsConstructor
public class MovieEventOutboxAdapter implements MovieEventPort {

    public static final String AGGREGATE_TYPE = "MOVIE";

    private final OutboxMessagePort outboxMessagePort;
    private final MovieKafkaProperties properties;
    private final JsonCodec json;
    private final Clock clock;

    /** Chụp toàn bộ thông tin phim (snapshot) thành message và lưu dòng PENDING. */
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
        outboxMessagePort.save(OutboxMessage.pending(
                AGGREGATE_TYPE,
                movie.getId(),
                type.name(),
                properties.movieEvents(),
                movie.getId().toString(),
                json.write(message)));
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
