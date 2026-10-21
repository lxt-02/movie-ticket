package com.mv.searchservice.indexing;

import com.mv.searchservice.elasticsearch.MovieIndexClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Applies a movie event to the index: archived movie -> remove, anything else -> upsert the snapshot.
 * Idempotent and order-safe thanks to the version (= movie.updatedAt) checked by Elasticsearch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieIndexer {

    static final String ARCHIVED = "ARCHIVED";

    private final MovieIndexClient indexClient;
    private final MovieDocumentMapper documentMapper;

    public void apply(MovieEvent event) {
        MovieEvent.Snapshot movie = event.payload();
        if (movie == null || movie.id() == null) {
            throw new IllegalArgumentException("Movie event " + event.eventId() + " has no movie payload");
        }
        String id = movie.id().toString();
        long version = version(event);
        if (ARCHIVED.equalsIgnoreCase(movie.status())) {
            boolean removed = indexClient.delete(id, version);
            log.info("INDEX {} movie={} -> {}", event.eventType(), id, removed ? "removed" : "already absent / newer");
            return;
        }
        boolean applied = indexClient.upsert(id, documentMapper.toDocument(movie), version);
        log.info("INDEX {} movie={} '{}' v{} -> {}", event.eventType(), id, movie.title(), version,
                applied ? "indexed" : "skipped (newer version already indexed)");
    }

    static long version(MovieEvent event) {
        if (event.version() != null && event.version() > 0) {
            return event.version();
        }
        MovieEvent.Snapshot m = event.payload();
        if (m.updatedAt() != null) {
            return m.updatedAt().toEpochMilli();
        }
        if (event.occurredAt() != null) {
            return event.occurredAt().toEpochMilli();
        }
        throw new IllegalArgumentException("Movie event " + event.eventId() + " has no version");
    }
}
