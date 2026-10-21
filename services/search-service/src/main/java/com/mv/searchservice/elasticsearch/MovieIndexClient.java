package com.mv.searchservice.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.mv.searchservice.config.JsonCodec;
import com.mv.searchservice.config.SearchProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Elasticsearch REST calls for the movie index.
 * <ul>
 *     <li>Writes use <b>external versioning</b> ({@code version = movie.updatedAt}, {@code version_type=external_gte}):
 *     Elasticsearch rejects (409) a snapshot older than the one already indexed, so a late or replayed event can
 *     never overwrite a newer change. A 409 is therefore "stale, ignore", not an error.</li>
 *     <li>The index is created with {@code elasticsearch/movies-index.json} the first time it is needed.</li>
 * </ul>
 */
@Slf4j
@Component
public class MovieIndexClient {

    static final String INDEX_DEFINITION = "elasticsearch/movies-index.json";

    private final RestClient client;
    private final JsonCodec json;
    private final String index;
    private final AtomicBoolean indexReady = new AtomicBoolean(false);

    public MovieIndexClient(RestClient elasticsearchRestClient, JsonCodec json, SearchProperties properties) {
        this.client = elasticsearchRestClient;
        this.json = json;
        this.index = properties.elasticsearch().movieIndex();
    }

    public String indexName() {
        return index;
    }

    /** Creates the index (settings + mapping) if it does not exist yet. */
    public void ensureIndex() {
        if (indexReady.get()) {
            return;
        }
        call("check index", () -> {
            try {
                client.head().uri("/{index}", index).retrieve().toBodilessEntity();
                log.info("Elasticsearch index '{}' exists", index);
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() != HttpStatus.NOT_FOUND.value()) {
                    throw e;
                }
                createIndex();
            }
            return null;
        });
        indexReady.set(true);
    }

    /** Deletes and recreates the index (empty). Used after a mapping change, followed by a resync from movie-service. */
    public void recreateIndex() {
        call("recreate index", () -> {
            try {
                client.delete().uri("/{index}", index).retrieve().toBodilessEntity();
                log.warn("Elasticsearch index '{}' deleted", index);
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() != HttpStatus.NOT_FOUND.value()) {
                    throw e;
                }
            }
            createIndex();
            return null;
        });
        indexReady.set(true);
    }

    /**
     * Index (create or replace) a document.
     *
     * @return false if Elasticsearch already holds a newer version (the document was left unchanged)
     */
    public boolean upsert(String id, Map<String, Object> document, long version) {
        ensureIndex();
        return call("index document " + id, () -> {
            try {
                client.put()
                        .uri("/{index}/_doc/{id}?version={version}&version_type=external_gte", index, id, version)
                        .body(json.write(document))
                        .retrieve()
                        .toBodilessEntity();
                return true;
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                    return false;
                }
                throw e;
            }
        });
    }

    /**
     * Remove a document.
     *
     * @return false if it was already absent, or a newer version is indexed
     */
    public boolean delete(String id, long version) {
        ensureIndex();
        return call("delete document " + id, () -> {
            try {
                client.delete()
                        .uri("/{index}/_doc/{id}?version={version}&version_type=external_gte", index, id, version)
                        .retrieve()
                        .toBodilessEntity();
                return true;
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (status == HttpStatus.NOT_FOUND.value() || status == HttpStatus.CONFLICT.value()) {
                    return false;
                }
                throw e;
            }
        });
    }

    /** POST /{index}/_search with the given query DSL; returns the raw response. */
    public JsonNode search(JsonNode body) {
        ensureIndex();
        return call("search", () -> json.readTree(client.post()
                .uri("/{index}/_search", index)
                .body(json.write(body))
                .retrieve()
                .body(String.class)));
    }

    private void createIndex() {
        try {
            client.put().uri("/{index}", index).body(loadIndexDefinition()).retrieve().toBodilessEntity();
            log.info("Elasticsearch index '{}' created from {}", index, INDEX_DEFINITION);
        } catch (RestClientResponseException e) {
            // Another search-service instance created it at the same moment.
            if (e.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()
                    && e.getResponseBodyAsString().contains("resource_already_exists_exception")) {
                return;
            }
            throw e;
        }
    }

    private static String loadIndexDefinition() {
        try (InputStream in = new ClassPathResource(INDEX_DEFINITION).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + INDEX_DEFINITION, e);
        }
    }

    /**
     * Connection problems and 5xx become {@link SearchUnavailableException} (retryable); other 4xx are bugs in the
     * request and are rethrown as {@link IllegalStateException} with Elasticsearch's explanation.
     */
    private <T> T call(String action, java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException e) {
            throw new SearchUnavailableException("Elasticsearch unreachable (" + action + "): " + e.getMessage(), e);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is5xxServerError() || e.getStatusCode().value() == 429) {
                throw new SearchUnavailableException("Elasticsearch error " + e.getStatusCode().value() + " ("
                        + action + "): " + e.getResponseBodyAsString(), e);
            }
            throw new IllegalStateException("Elasticsearch rejected " + action + " (" + e.getStatusCode().value()
                    + "): " + e.getResponseBodyAsString(), e);
        }
    }
}
