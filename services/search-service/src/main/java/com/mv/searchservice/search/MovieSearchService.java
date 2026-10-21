package com.mv.searchservice.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.mv.searchservice.elasticsearch.MovieIndexClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MovieSearchService {

    static final int MAX_SUGGESTIONS = 10;

    private final MovieIndexClient indexClient;
    private final MovieSearchQueryBuilder queryBuilder;

    public MovieSearchResult search(MovieSearchCriteria criteria) {
        JsonNode response = indexClient.search(queryBuilder.build(criteria));
        return toResult(response, criteria);
    }

    public List<MovieSearchResult.Suggestion> suggest(String prefix, int size) {
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }
        int limit = Math.min(MAX_SUGGESTIONS, Math.max(1, size));
        JsonNode response = indexClient.search(queryBuilder.suggest(prefix.trim(), limit));
        List<MovieSearchResult.Suggestion> suggestions = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) {
            JsonNode s = hit.path("_source");
            suggestions.add(new MovieSearchResult.Suggestion(text(s, "id"), text(s, "title"), text(s, "originalTitle"),
                    text(s, "posterUrl"), date(s, "releaseDate"), text(s, "status")));
        }
        return suggestions;
    }

    MovieSearchResult toResult(JsonNode response, MovieSearchCriteria criteria) {
        JsonNode hits = response.path("hits");
        long total = hits.path("total").path("value").asLong(0);
        List<MovieSearchResult.Hit> items = new ArrayList<>();
        for (JsonNode hit : hits.path("hits")) {
            JsonNode s = hit.path("_source");
            JsonNode score = hit.path("_score");
            items.add(new MovieSearchResult.Hit(
                    text(s, "id"), text(s, "title"), text(s, "originalTitle"), text(s, "description"),
                    text(s, "posterUrl"), list(s.path("genres")), list(s.path("directors")), list(s.path("actors")),
                    text(s, "ageRating"), text(s, "language"), text(s, "country"), text(s, "status"),
                    date(s, "releaseDate"), s.hasNonNull("durationMinutes") ? s.get("durationMinutes").asInt() : null,
                    score.isNumber() ? score.asDouble() : null,
                    highlight(hit.path("highlight"))));
        }
        Map<String, List<MovieSearchResult.FacetBucket>> facets = new LinkedHashMap<>();
        JsonNode aggs = response.path("aggregations");
        for (String facet : MovieSearchQueryBuilder.FACET_ORDER) {
            List<MovieSearchResult.FacetBucket> buckets = new ArrayList<>();
            for (JsonNode b : aggs.path(facet).path("values").path("buckets")) {
                buckets.add(new MovieSearchResult.FacetBucket(b.path("key").asText(), b.path("doc_count").asLong()));
            }
            facets.put(facet, buckets);
        }
        int totalPages = (int) Math.ceil((double) total / criteria.size());
        return new MovieSearchResult(items, criteria.page(), criteria.size(), total, totalPages, facets,
                response.path("took").asLong(0));
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static LocalDate date(JsonNode node, String field) {
        String v = text(node, field);
        return v == null ? null : LocalDate.parse(v);
    }

    private static List<String> list(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (array.isArray()) {
            array.forEach(v -> values.add(v.asText()));
        } else if (array.isTextual()) {
            values.add(array.asText());
        }
        return values;
    }

    private static Map<String, List<String>> highlight(JsonNode node) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        node.fields().forEachRemaining(e -> map.put(e.getKey(), list(e.getValue())));
        return map;
    }
}
