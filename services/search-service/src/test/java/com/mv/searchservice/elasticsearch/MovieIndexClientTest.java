package com.mv.searchservice.elasticsearch;

import com.mv.searchservice.config.JsonCodec;
import com.mv.searchservice.config.SearchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** The exact HTTP calls sent to Elasticsearch. */
class MovieIndexClientTest {

    private static final String ES = "http://es:9200";

    private MockRestServiceServer server;
    private MovieIndexClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(ES);
        server = MockRestServiceServer.bindTo(builder).build();
        SearchProperties properties = new SearchProperties(
                new SearchProperties.Elasticsearch(ES, null, null, "movies", null, null), null);
        client = new MovieIndexClient(builder.build(), new JsonCodec(), properties);
    }

    private static final String GOOD_MAPPING =
            "{\"movies\":{\"mappings\":{\"dynamic\":\"strict\",\"properties\":{\"id\":{\"type\":\"keyword\"}}}}}";
    /** What Elasticsearch guesses when a document is written into a missing index. */
    private static final String AUTO_CREATED_MAPPING =
            "{\"movies\":{\"mappings\":{\"properties\":{\"id\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}}}}}}";

    private void expectIndexExists() {
        server.expect(requestTo(ES + "/movies")).andExpect(method(HttpMethod.HEAD)).andRespond(withSuccess());
        server.expect(requestTo(ES + "/movies/_mapping")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(GOOD_MAPPING, MediaType.APPLICATION_JSON));
    }

    private void expectIndexCreated() {
        server.expect(requestTo(ES + "/movies")).andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"strict\"")))
                .andRespond(withSuccess("{\"acknowledged\":true}", MediaType.APPLICATION_JSON));
    }

    @Test
    void recreatesAnIndexThatElasticsearchAutoCreatedWithTheWrongMapping() {
        server.expect(requestTo(ES + "/movies")).andExpect(method(HttpMethod.HEAD)).andRespond(withSuccess());
        server.expect(requestTo(ES + "/movies/_mapping")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(AUTO_CREATED_MAPPING, MediaType.APPLICATION_JSON));
        server.expect(requestTo(ES + "/movies")).andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("{\"acknowledged\":true}", MediaType.APPLICATION_JSON));
        expectIndexCreated();

        client.ensureIndex();
        client.ensureIndex(); // repaired once, then cached

        server.verify();
    }

    @Test
    void recreatesAMissingIndexAndRetriesTheWriteOnce() {
        expectIndexExists();
        server.expect(requestTo(ES + "/movies/_doc/m1?version=7&version_type=external_gte"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .body("{\"error\":{\"type\":\"index_not_found_exception\"},\"status\":404}")
                        .contentType(MediaType.APPLICATION_JSON));
        server.expect(requestTo(ES + "/movies")).andExpect(method(HttpMethod.HEAD))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        expectIndexCreated();
        server.expect(requestTo(ES + "/movies/_doc/m1?version=7&version_type=external_gte"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{\"result\":\"created\"}", MediaType.APPLICATION_JSON));

        assertThat(client.upsert("m1", Map.of("id", "m1"), 7)).isTrue();
        server.verify();
    }

    @Test
    void recreateIndexDeletesThenCreates() {
        server.expect(requestTo(ES + "/movies")).andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("{\"acknowledged\":true}", MediaType.APPLICATION_JSON));
        expectIndexCreated();
        server.expect(requestTo(ES + "/movies/_search")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"took\":1}", MediaType.APPLICATION_JSON));

        client.recreateIndex();
        client.search(new JsonCodec().object()); // no second check: index is known to be ready

        server.verify();
    }

    @Test
    void createsTheIndexWithTheMappingWhenItIsMissing() {
        server.expect(requestTo(ES + "/movies")).andExpect(method(HttpMethod.HEAD))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(ES + "/movies")).andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"vi_folding\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"autocomplete_index\"")))
                .andRespond(withSuccess("{\"acknowledged\":true}", MediaType.APPLICATION_JSON));

        client.ensureIndex();
        client.ensureIndex(); // checked once, then cached

        server.verify();
    }

    @Test
    void upsertUsesExternalVersioningAndTreatsConflictAsStale() {
        expectIndexExists();
        server.expect(requestTo(ES + "/movies/_doc/m1?version=1700000000000&version_type=external_gte"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"id\":\"m1\",\"title\":\"Dune\"}"))
                .andRespond(withSuccess("{\"result\":\"created\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ES + "/movies/_doc/m1?version=1600000000000&version_type=external_gte"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .body("{\"error\":{\"type\":\"version_conflict_engine_exception\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThat(client.upsert("m1", Map.of("id", "m1", "title", "Dune"), 1_700_000_000_000L)).isTrue();
        assertThat(client.upsert("m1", Map.of("id", "m1", "title", "Old"), 1_600_000_000_000L)).isFalse();
        server.verify();
    }

    @Test
    void deleteIgnoresAnAlreadyMissingDocument() {
        expectIndexExists();
        server.expect(requestTo(ES + "/movies/_doc/m1?version=5&version_type=external_gte"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.delete("m1", 5)).isFalse();
        server.verify();
    }

    @Test
    void serverErrorsAreRetryableAndBadRequestsAreNot() {
        expectIndexExists();
        server.expect(requestTo(ES + "/movies/_search")).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(ES + "/movies/_search")).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\":\"parsing_exception\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        JsonCodec json = new JsonCodec();
        assertThatThrownBy(() -> client.search(json.object())).isInstanceOf(SearchUnavailableException.class);
        assertThatThrownBy(() -> client.search(json.object()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("parsing_exception");
        server.verify();
    }

    @Test
    void searchReturnsTheRawResponse() {
        expectIndexExists();
        server.expect(requestTo(ES + "/movies/_search")).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"size\":3}"))
                .andRespond(withSuccess("{\"took\":4,\"hits\":{\"total\":{\"value\":0},\"hits\":[]}}", MediaType.APPLICATION_JSON));

        JsonCodec json = new JsonCodec();
        assertThat(client.search(json.object().put("size", 3)).path("took").asInt()).isEqualTo(4);
        server.verify();
    }
}
