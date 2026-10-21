package com.mv.searchservice.indexing;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Message of topic movie-events (produced by movie-service, key = movieId). Every event carries the full movie
 * snapshot; {@code version} is the movie's updatedAt in epoch milliseconds.
 */
public record MovieEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        Instant occurredAt,
        Long version,
        Snapshot payload
) {

    public record Snapshot(
            UUID id,
            String title,
            String originalTitle,
            String description,
            Integer durationMinutes,
            LocalDate releaseDate,
            String ageRating,
            String language,
            String country,
            String posterUrl,
            String status,
            List<Ref> genres,
            List<Ref> directors,
            List<Ref> actors,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    /** A genre ({@code characterName} null), a director, or an actor. */
    public record Ref(UUID id, String name, String characterName) {
    }
}
