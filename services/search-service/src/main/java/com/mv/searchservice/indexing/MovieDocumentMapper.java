package com.mv.searchservice.indexing;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Movie snapshot -> Elasticsearch document (fields of {@code elasticsearch/movies-index.json}).
 * The document is denormalized for search: genre / director / actor names are stored directly on the movie.
 */
@Component
public class MovieDocumentMapper {

    public Map<String, Object> toDocument(MovieEvent.Snapshot m) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", m.id().toString());
        doc.put("title", m.title());
        doc.put("originalTitle", m.originalTitle());
        doc.put("description", m.description());
        doc.put("genres", names(m.genres()));
        doc.put("genreIds", values(m.genres(), r -> r.id() == null ? null : r.id().toString()));
        doc.put("directors", names(m.directors()));
        doc.put("actors", names(m.actors()));
        doc.put("characters", values(m.actors(), MovieEvent.Ref::characterName));
        doc.put("ageRating", m.ageRating());
        doc.put("language", m.language());
        doc.put("country", m.country());
        doc.put("status", m.status());
        doc.put("releaseDate", m.releaseDate() != null ? m.releaseDate().toString() : null);
        doc.put("durationMinutes", m.durationMinutes());
        doc.put("posterUrl", m.posterUrl());
        doc.put("createdAt", m.createdAt() != null ? m.createdAt().toString() : null);
        doc.put("updatedAt", m.updatedAt() != null ? m.updatedAt().toString() : null);
        return doc;
    }

    private static List<String> names(List<MovieEvent.Ref> refs) {
        return values(refs, MovieEvent.Ref::name);
    }

    private static List<String> values(List<MovieEvent.Ref> refs, Function<MovieEvent.Ref, String> field) {
        if (refs == null) {
            return List.of();
        }
        return refs.stream().map(field).filter(Objects::nonNull).filter(s -> !s.isBlank()).distinct().toList();
    }
}
