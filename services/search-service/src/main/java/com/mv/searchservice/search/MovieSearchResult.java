package com.mv.searchservice.search;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * One page of search results with facet counts.
 *
 * @param facets facet name (genres, status, ageRating, language, country) -> buckets, most frequent first
 */
public record MovieSearchResult(
        List<Hit> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        Map<String, List<FacetBucket>> facets,
        long tookMs
) {

    /**
     * @param highlight field -> fragments with the matched words wrapped in &lt;em&gt;...&lt;/em&gt;
     */
    public record Hit(
            String id,
            String title,
            String originalTitle,
            String description,
            String posterUrl,
            List<String> genres,
            List<String> directors,
            List<String> actors,
            String ageRating,
            String language,
            String country,
            String status,
            LocalDate releaseDate,
            Integer durationMinutes,
            Double score,
            Map<String, List<String>> highlight
    ) {
    }

    public record FacetBucket(String value, long count) {
    }

    public record Suggestion(String id, String title, String originalTitle, String posterUrl,
                             LocalDate releaseDate, String status) {
    }
}
