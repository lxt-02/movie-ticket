package com.mv.searchservice.search;

import com.mv.searchservice.config.JsonCodec;
import com.mv.searchservice.elasticsearch.MovieIndexClient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Elasticsearch response -> API result (hits, highlight, facets, paging). */
class MovieSearchServiceTest {

    static final String RESPONSE = """
            {"took":7,"hits":{"total":{"value":13,"relation":"eq"},"hits":[
              {"_id":"m4","_score":8.2,
               "_source":{"id":"m4","title":"Lật Mặt 7: Một Điều Ước","genres":["Drama","Family"],
                          "directors":["Lý Hải"],"actors":[],"ageRating":"K","status":"NOW_SHOWING",
                          "releaseDate":"2024-04-26","durationMinutes":138,"posterUrl":"p.jpg"},
               "highlight":{"title":["<em>Lật</em> <em>Mặt</em> 7: Một Điều Ước"]}}]},
             "aggregations":{
               "genres":{"doc_count":13,"values":{"buckets":[{"key":"Drama","doc_count":9},{"key":"Family","doc_count":4}]}},
               "status":{"doc_count":13,"values":{"buckets":[{"key":"NOW_SHOWING","doc_count":13}]}},
               "ageRating":{"doc_count":13,"values":{"buckets":[]}},
               "language":{"doc_count":13,"values":{"buckets":[]}},
               "country":{"doc_count":13,"values":{"buckets":[{"key":"Vietnam","doc_count":13}]}}}}
            """;

    @Test
    void mapsHitsHighlightsFacetsAndPaging() {
        JsonCodec json = new JsonCodec();
        MovieIndexClient client = mock(MovieIndexClient.class);
        when(client.search(any())).thenReturn(json.readTree(RESPONSE));
        MovieSearchService service = new MovieSearchService(client, new MovieSearchQueryBuilder(json));

        MovieSearchResult result = service.search(new MovieSearchCriteria("lat mat", null, null, null, null, null,
                null, null, null, null, null, null, 1, 5));

        assertThat(result.totalElements()).isEqualTo(13);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.tookMs()).isEqualTo(7);
        MovieSearchResult.Hit hit = result.items().get(0);
        assertThat(hit.title()).isEqualTo("Lật Mặt 7: Một Điều Ước");
        assertThat(hit.genres()).containsExactly("Drama", "Family");
        assertThat(hit.releaseDate()).isEqualTo(LocalDate.of(2024, 4, 26));
        assertThat(hit.durationMinutes()).isEqualTo(138);
        assertThat(hit.originalTitle()).isNull();
        assertThat(hit.score()).isEqualTo(8.2);
        assertThat(hit.highlight().get("title")).containsExactly("<em>Lật</em> <em>Mặt</em> 7: Một Điều Ước");
        assertThat(result.facets()).containsOnlyKeys("genres", "status", "ageRating", "language", "country");
        assertThat(result.facets().get("genres")).containsExactly(
                new MovieSearchResult.FacetBucket("Drama", 9), new MovieSearchResult.FacetBucket("Family", 4));
        assertThat(result.facets().get("ageRating")).isEmpty();
    }

    @Test
    void blankSuggestPrefixDoesNotCallElasticsearch() {
        MovieIndexClient client = mock(MovieIndexClient.class);
        MovieSearchService service = new MovieSearchService(client, new MovieSearchQueryBuilder(new JsonCodec()));
        assertThat(service.suggest("  ", 5)).isEqualTo(List.of());
    }
}
