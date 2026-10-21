package com.mv.searchservice.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mv.searchservice.config.JsonCodec;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovieSearchQueryBuilderTest {

    private final MovieSearchQueryBuilder builder = new MovieSearchQueryBuilder(new JsonCodec());

    private static MovieSearchCriteria criteria(String q, List<String> genres, List<String> ageRatings, String sort,
                                                int page, int size) {
        return new MovieSearchCriteria(q, null, genres, List.of("now_showing"), ageRatings, null, null,
                LocalDate.of(2024, 1, 1), null, 90, 180, MovieSearchCriteria.Sort.parse(sort), page, size);
    }

    @Test
    void keywordSearchWithFacetFiltersRangesHighlightAndPaging() {
        ObjectNode body = builder.build(criteria("lat mat", List.of("Drama", "Family"), List.of("k"), null, 2, 10));

        assertThat(body.get("from").asInt()).isEqualTo(20);
        assertThat(body.get("size").asInt()).isEqualTo(10);

        JsonNode must = body.at("/query/bool/must");
        assertThat(must).hasSize(1);
        JsonNode fuzzy = must.get(0).at("/bool/should/0/multi_match");
        assertThat(fuzzy.get("query").asText()).isEqualTo("lat mat");
        assertThat(fuzzy.get("fuzziness").asText()).isEqualTo("AUTO");
        assertThat(texts(fuzzy.get("fields"))).contains("title^4", "actors^2", "description");
        assertThat(must.get(0).at("/bool/should/1/multi_match/type").asText()).isEqualTo("phrase_prefix");

        JsonNode filter = body.at("/query/bool/filter");
        assertThat(filter.get(0).at("/range/releaseDate/gte").asText()).isEqualTo("2024-01-01");
        assertThat(filter.get(0).at("/range/releaseDate").has("lte")).isFalse();
        assertThat(filter.get(1).at("/range/durationMinutes/gte").asInt()).isEqualTo(90);
        assertThat(filter.get(1).at("/range/durationMinutes/lte").asInt()).isEqualTo(180);

        // Facet criteria live in post_filter (all of them), values upper-cased where they are codes.
        JsonNode postFilters = body.at("/post_filter/bool/filter");
        assertThat(postFilters).hasSize(3);
        assertThat(texts(postFilters.get(0).at("/terms/genres"))).containsExactly("Drama", "Family");
        assertThat(texts(postFilters.get(1).at("/terms/status"))).containsExactly("NOW_SHOWING");
        assertThat(texts(postFilters.get(2).at("/terms/ageRating"))).containsExactly("K");

        // Each facet ignores its own criterion but applies the others (multi-select facets).
        JsonNode genreFacetFilters = body.at("/aggs/genres/filter/bool/filter");
        assertThat(genreFacetFilters).hasSize(2);
        assertThat(genreFacetFilters.toString()).doesNotContain("\"genres\"").contains("NOW_SHOWING", "\"K\"");
        assertThat(body.at("/aggs/genres/aggs/values/terms/field").asText()).isEqualTo("genres");
        JsonNode statusFacetFilters = body.at("/aggs/status/filter/bool/filter");
        assertThat(statusFacetFilters.toString()).contains("Drama").doesNotContain("NOW_SHOWING");
        assertThat(body.at("/aggs/country/filter/bool/filter")).hasSize(3);

        assertThat(body.at("/highlight/fields").has("title")).isTrue();
        assertThat(body.at("/sort/0").has("_score")).isTrue();
        assertThat(body.at("/sort/2/id/order").asText()).isEqualTo("asc");
    }

    @Test
    void noKeywordMatchesAllAndSortsNewestFirst() {
        ObjectNode body = builder.build(new MovieSearchCriteria(null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 12));

        assertThat(body.at("/query/bool/must/0").has("match_all")).isTrue();
        assertThat(body.at("/query/bool/filter")).isEmpty();
        assertThat(body.at("/post_filter/bool/filter")).isEmpty();
        assertThat(body.at("/sort/0/releaseDate/order").asText()).isEqualTo("desc");
        assertThat(body.at("/sort/0/releaseDate/missing").asText()).isEqualTo("_last");
    }

    @Test
    void personSearchAndExplicitSort() {
        ObjectNode body = builder.build(new MovieSearchCriteria(null, "villeneuve", null, null, null, null, null,
                null, null, null, null, MovieSearchCriteria.Sort.parse("duration_desc"), 0, 5));

        assertThat(texts(body.at("/query/bool/must/0/multi_match/fields"))).containsExactly("directors", "actors");
        assertThat(body.at("/sort/0/durationMinutes/order").asText()).isEqualTo("desc");
    }

    @Test
    void commaSeparatedValuesAreSplitAndDeduplicated() {
        MovieSearchCriteria c = new MovieSearchCriteria(" ", null, List.of("Action, Drama", "Action"), null, null,
                null, null, null, null, null, null, null, 0, 10);
        assertThat(c.keyword()).isNull();
        assertThat(c.genres()).containsExactly("Action", "Drama");
    }

    @Test
    void invalidCriteriaAreRejected() {
        assertThatThrownBy(() -> MovieSearchCriteria.Sort.parse("popular")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MovieSearchCriteria(null, null, null, null, null, null, null,
                LocalDate.of(2025, 1, 1), LocalDate.of(2024, 1, 1), null, null, null, 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MovieSearchCriteria(null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 51))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void suggestQueryUsesTheEdgeNgramFields() {
        ObjectNode body = builder.suggest("lat ma", 5);
        assertThat(body.get("size").asInt()).isEqualTo(5);
        assertThat(body.at("/query/bool/should/0/match").has("title.suggest")).isTrue();
        assertThat(body.at("/query/bool/should/1/match").has("originalTitle.suggest")).isTrue();
        assertThat(texts(body.get("_source"))).contains("id", "title", "posterUrl");
    }

    private static List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(v -> values.add(v.asText()));
        return values;
    }
}
