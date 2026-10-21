package com.mv.searchservice.indexing;

import com.mv.searchservice.config.JsonCodec;
import com.mv.searchservice.elasticsearch.MovieIndexClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MovieIndexerTest {

    /** Exactly what movie-service writes to the outbox (MovieEventOutboxAdapter). */
    static final String CREATED = """
            {"eventId":"9b2f8a3e-1111-4c1b-9d0e-7a2f3b4c5d6e","eventType":"MOVIE_CREATED",
             "aggregateId":"20000000-0000-0000-0000-000000000004","occurredAt":"2026-10-21T08:00:00Z",
             "version":1761033600000,
             "payload":{"id":"20000000-0000-0000-0000-000000000004","title":"Lật Mặt 7: Một Điều Ước",
               "originalTitle":null,"description":"Câu chuyện gia đình","durationMinutes":138,
               "releaseDate":"2024-04-26","ageRating":"K","language":"Vietnamese","country":"Vietnam",
               "posterUrl":"https://example.com/p.jpg","status":"NOW_SHOWING",
               "genres":[{"id":"21000000-0000-0000-0000-000000000005","name":"Drama"},
                         {"id":"21000000-0000-0000-0000-000000000007","name":"Family"}],
               "directors":[{"id":"22000000-0000-0000-0000-000000000009","name":"Lý Hải"}],
               "actors":[{"id":"22000000-0000-0000-0000-000000000012","name":"Thanh Hiền","characterName":"Bà Hai"}],
               "createdAt":"2026-10-21T08:00:00Z","updatedAt":"2026-10-21T08:00:00Z"}}
            """;

    private final MovieIndexClient indexClient = mock(MovieIndexClient.class);
    private final MovieIndexer indexer = new MovieIndexer(indexClient, new MovieDocumentMapper());
    private final JsonCodec json = new JsonCodec();

    @SuppressWarnings("unchecked")
    @Test
    void upsertsTheDenormalizedDocumentWithTheEventVersion() {
        indexer.apply(json.read(CREATED, MovieEvent.class));

        ArgumentCaptor<Map<String, Object>> doc = ArgumentCaptor.forClass(Map.class);
        verify(indexClient).upsert(eq("20000000-0000-0000-0000-000000000004"), doc.capture(), eq(1761033600000L));
        Map<String, Object> d = doc.getValue();
        assertThat(d.get("title")).isEqualTo("Lật Mặt 7: Một Điều Ước");
        assertThat(d.get("genres")).isEqualTo(List.of("Drama", "Family"));
        assertThat(d.get("genreIds")).isEqualTo(List.of("21000000-0000-0000-0000-000000000005",
                "21000000-0000-0000-0000-000000000007"));
        assertThat(d.get("directors")).isEqualTo(List.of("Lý Hải"));
        assertThat(d.get("actors")).isEqualTo(List.of("Thanh Hiền"));
        assertThat(d.get("characters")).isEqualTo(List.of("Bà Hai"));
        assertThat(d.get("releaseDate")).isEqualTo("2024-04-26");
        assertThat(d.get("durationMinutes")).isEqualTo(138);
        verify(indexClient, never()).delete(anyString(), anyLong());
    }

    @Test
    void archivedMovieIsRemovedFromTheIndex() {
        String archived = CREATED.replace("\"MOVIE_CREATED\"", "\"MOVIE_ARCHIVED\"")
                .replace("\"NOW_SHOWING\"", "\"ARCHIVED\"");

        indexer.apply(json.read(archived, MovieEvent.class));

        verify(indexClient).delete("20000000-0000-0000-0000-000000000004", 1761033600000L);
        verify(indexClient, never()).upsert(anyString(), any(), anyLong());
    }

    @Test
    void versionFallsBackToUpdatedAt() {
        MovieEvent event = json.read(CREATED.replace("\"version\":1761033600000,", ""), MovieEvent.class);
        assertThat(MovieIndexer.version(event)).isEqualTo(java.time.Instant.parse("2026-10-21T08:00:00Z").toEpochMilli());
    }

    @Test
    void malformedMessagesAreRejected() {
        assertThatThrownBy(() -> json.read("{not json", MovieEvent.class)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> indexer.apply(json.read("{\"eventType\":\"MOVIE_CREATED\"}", MovieEvent.class)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
