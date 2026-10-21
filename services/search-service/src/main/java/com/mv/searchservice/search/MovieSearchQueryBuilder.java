package com.mv.searchservice.search;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mv.searchservice.config.JsonCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the Elasticsearch query DSL for a movie search.
 * <pre>
 * {
 *   "query":       keyword / person (scored) + release-date and duration ranges (filter, not scored),
 *   "post_filter": genres, status, ageRating, language, country (the facet criteria),
 *   "aggs":        one facet per criterion, each filtered by all OTHER facet criteria,
 *   "highlight", "sort", "from", "size"
 * }
 * </pre>
 * Facet criteria go in {@code post_filter} instead of the query so that a facet still shows the other values of
 * its own criterion ("multi-select facets"): after ticking "Action", the genre facet keeps showing
 * "Drama (3)" so the user can add it, while the status facet only counts Action movies.
 */
@Component
@RequiredArgsConstructor
public class MovieSearchQueryBuilder {

    /** Facet name -> keyword field in the index. */
    public static final Map<String, String> FACETS = Map.of(
            "genres", "genres",
            "status", "status",
            "ageRating", "ageRating",
            "language", "language",
            "country", "country"
    );
    static final List<String> FACET_ORDER = List.of("genres", "status", "ageRating", "language", "country");
    static final int FACET_SIZE = 30;

    static final String[] KEYWORD_FIELDS = {
            "title^4", "originalTitle^3", "directors^2", "actors^2", "characters", "description"
    };

    private final JsonCodec json;

    public ObjectNode build(MovieSearchCriteria c) {
        ObjectNode body = json.object();
        body.put("from", c.page() * c.size());
        body.put("size", c.size());
        body.put("track_total_hits", true);

        ObjectNode bool = body.putObject("query").putObject("bool");
        ArrayNode must = bool.putArray("must");
        if (c.keyword() != null) {
            must.add(keywordQuery(c.keyword()));
        }
        if (c.person() != null) {
            must.add(personQuery(c.person()));
        }
        if (must.isEmpty()) {
            must.addObject().putObject("match_all");
        }
        ArrayNode filter = bool.putArray("filter");
        if (c.releaseFrom() != null || c.releaseTo() != null) {
            ObjectNode range = filter.addObject().putObject("range").putObject("releaseDate");
            if (c.releaseFrom() != null) {
                range.put("gte", c.releaseFrom().toString());
            }
            if (c.releaseTo() != null) {
                range.put("lte", c.releaseTo().toString());
            }
        }
        if (c.durationMin() != null || c.durationMax() != null) {
            ObjectNode range = filter.addObject().putObject("range").putObject("durationMinutes");
            if (c.durationMin() != null) {
                range.put("gte", c.durationMin());
            }
            if (c.durationMax() != null) {
                range.put("lte", c.durationMax());
            }
        }

        Map<String, List<String>> selected = selectedFacets(c);
        body.set("post_filter", facetFilter(selected, null));

        ObjectNode aggs = body.putObject("aggs");
        for (String facet : FACET_ORDER) {
            ObjectNode agg = aggs.putObject(facet);
            agg.set("filter", facetFilter(selected, facet));
            agg.putObject("aggs").putObject("values").putObject("terms")
                    .put("field", FACETS.get(facet))
                    .put("size", FACET_SIZE);
        }

        ObjectNode highlight = body.putObject("highlight");
        highlight.putArray("pre_tags").add("<em>");
        highlight.putArray("post_tags").add("</em>");
        ObjectNode fields = highlight.putObject("fields");
        fields.putObject("title").put("number_of_fragments", 0);
        fields.putObject("originalTitle").put("number_of_fragments", 0);
        fields.putObject("directors").put("number_of_fragments", 0);
        fields.putObject("actors").put("number_of_fragments", 0);
        fields.putObject("description").put("fragment_size", 160).put("number_of_fragments", 1);

        body.set("sort", sort(c.effectiveSort()));
        return body;
    }

    /** Autocomplete: prefix match on the edge-ngram sub-fields, typo-tolerant match on the title. */
    public ObjectNode suggest(String prefix, int size) {
        ObjectNode body = json.object();
        body.put("size", size);
        body.putArray("_source").add("id").add("title").add("originalTitle").add("posterUrl")
                .add("releaseDate").add("status");
        ObjectNode bool = body.putObject("query").putObject("bool");
        ArrayNode should = bool.putArray("should");
        should.addObject().putObject("match").putObject("title.suggest")
                .put("query", prefix).put("operator", "and").put("boost", 3);
        should.addObject().putObject("match").putObject("originalTitle.suggest")
                .put("query", prefix).put("operator", "and").put("boost", 2);
        should.addObject().putObject("match").putObject("title")
                .put("query", prefix).put("fuzziness", "AUTO").put("prefix_length", 1);
        bool.put("minimum_should_match", 1);
        return body;
    }

    /**
     * Free text. Accents are folded at index and query time ("lat mat" finds "Lật Mặt"), typos are tolerated
     * (fuzziness AUTO), and an unfinished last word still matches (phrase_prefix / edge n-grams).
     */
    ObjectNode keywordQuery(String keyword) {
        ObjectNode bool = json.object();
        ObjectNode inner = bool.putObject("bool");
        ArrayNode should = inner.putArray("should");

        ObjectNode fuzzy = should.addObject().putObject("multi_match");
        fuzzy.put("query", keyword);
        ArrayNode f1 = fuzzy.putArray("fields");
        for (String field : KEYWORD_FIELDS) {
            f1.add(field);
        }
        fuzzy.put("type", "best_fields");
        // 1-2 words: all must match; 3+ words: 75% of them (tolerates an extra word like "phim").
        fuzzy.put("minimum_should_match", "2<75%");
        fuzzy.put("fuzziness", "AUTO");
        fuzzy.put("prefix_length", 1);

        ObjectNode prefix = should.addObject().putObject("multi_match");
        prefix.put("query", keyword);
        prefix.putArray("fields").add("title^6").add("originalTitle^4");
        prefix.put("type", "phrase_prefix");

        ObjectNode partial = should.addObject().putObject("match").putObject("title.suggest");
        partial.put("query", keyword);
        partial.put("operator", "and");
        partial.put("boost", 2);

        inner.put("minimum_should_match", 1);
        return bool;
    }

    ObjectNode personQuery(String person) {
        ObjectNode node = json.object();
        ObjectNode mm = node.putObject("multi_match");
        mm.put("query", person);
        mm.putArray("fields").add("directors").add("actors");
        mm.put("operator", "and");
        mm.put("fuzziness", "AUTO");
        return node;
    }

    /** bool.filter of terms queries for the selected facets, except {@code excluded} (null = keep all). */
    ObjectNode facetFilter(Map<String, List<String>> selected, String excluded) {
        ObjectNode node = json.object();
        ArrayNode filters = node.putObject("bool").putArray("filter");
        selected.forEach((facet, values) -> {
            if (!facet.equals(excluded) && !values.isEmpty()) {
                ArrayNode terms = filters.addObject().putObject("terms").putArray(FACETS.get(facet));
                values.forEach(terms::add);
            }
        });
        return node;
    }

    static Map<String, List<String>> selectedFacets(MovieSearchCriteria c) {
        Map<String, List<String>> selected = new LinkedHashMap<>();
        selected.put("genres", c.genres());
        selected.put("status", c.statuses());
        selected.put("ageRating", c.ageRatings());
        selected.put("language", c.languages());
        selected.put("country", c.countries());
        return selected;
    }

    private ArrayNode sort(MovieSearchCriteria.Sort sort) {
        ArrayNode array = json.array();
        switch (sort) {
            case RELEVANCE -> {
                array.addObject().putObject("_score").put("order", "desc");
                array.add(dateSort("desc"));
            }
            case NEWEST -> array.add(dateSort("desc"));
            case OLDEST -> array.add(dateSort("asc"));
            case TITLE -> array.addObject().putObject("title.keyword").put("order", "asc");
            case DURATION_ASC -> array.addObject().putObject("durationMinutes").put("order", "asc");
            case DURATION_DESC -> array.addObject().putObject("durationMinutes").put("order", "desc");
        }
        array.addObject().putObject("id").put("order", "asc");   // stable order across pages
        return array;
    }

    private ObjectNode dateSort(String order) {
        ObjectNode node = json.object();
        node.putObject("releaseDate").put("order", order).put("missing", "_last");
        return node;
    }
}
