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
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Elasticsearch REST calls for the movie index.
 * <ul>
 *     <li>Writes use <b>external versioning</b> ({@code version = movie.updatedAt}, {@code version_type=external_gte}):
 *     Elasticsearch rejects (409) a snapshot older than the one already indexed, so a late or replayed event can
 *     never overwrite a newer change. A 409 is therefore "stale, ignore", not an error.</li>
 *     <li>The index is created with {@code elasticsearch/movies-index.json} the first time it is needed.</li>
 *     <li><b>The mapping is checked</b>, not only the existence of the index. If a document is written while the
 *     index does not exist, Elasticsearch auto-creates it with a guessed (dynamic) mapping: {@code id} becomes a
 *     {@code text} field and every search fails ("Fielddata is disabled on [id]"). Such an index is detected
 *     (our mapping is {@code "dynamic": "strict"} with {@code id} = {@code keyword}) and recreated.</li>
 *     <li>Recreating the index holds a write lock, document writes and searches a read lock, so no document can be
 *     written between "delete index" and "create index" (that race used to produce the auto-created index).</li>
 * </ul>
 * After the index has been (re)created it is empty: {@code POST /api/v1/movies/search-sync} (ADMIN) or a restart of
 * movie-service re-publishes every movie.
 */
@Slf4j
@Component
public class MovieIndexClient {

    static final String INDEX_DEFINITION = "elasticsearch/movies-index.json";

    private final RestClient client;
    private final JsonCodec json;
    private final String index;
    private final AtomicBoolean indexReady = new AtomicBoolean(false);
    /** read = use the index (write documents, search); write = check / create / delete the index */
    private final ReentrantReadWriteLock indexLock = new ReentrantReadWriteLock();

    public MovieIndexClient(RestClient elasticsearchRestClient, JsonCodec json, SearchProperties properties) {
        this.client = elasticsearchRestClient;
        this.json = json;
        this.index = properties.elasticsearch().movieIndex();
    }

    public String indexName() {
        return index;
    }

    /** Creates the index (settings + mapping) if it does not exist yet, or recreates it if its mapping is wrong. */
    public void ensureIndex() {
        if (indexReady.get()) {
            return;
        }
        indexLock.writeLock().lock();
        try {
            if (indexReady.get()) {
                return;
            }
            call("check index", () -> {
                if (!indexExists()) {
                    createIndex();
                } else if (!hasExpectedMapping()) {
                    log.error("Elasticsearch index '{}' has a wrong mapping (auto-created by Elasticsearch, not from {}). "
                            + "Recreating it EMPTY: call POST /api/v1/movies/search-sync (ADMIN) or restart movie-service "
                            + "to index the movies again.", index, INDEX_DEFINITION);
                    deleteIndex();
                    createIndex();
                } else {
                    log.info("Elasticsearch index '{}' exists with the expected mapping", index);
                }
                return null;
            });
            indexReady.set(true);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    /** Deletes and recreates the index (empty). Used after a mapping change, followed by a resync from movie-service. */
    public void recreateIndex() {
        indexLock.writeLock().lock();
        try {
            indexReady.set(false);
            call("recreate index", () -> {
                deleteIndex();
                createIndex();
                return null;
            });
            indexReady.set(true);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    /**
     * Index (create or replace) a document.
     *
     * @return false if Elasticsearch already holds a newer version (the document was left unchanged)
     */
    public boolean upsert(String id, Map<String, Object> document, long version) {
        return withIndex("index document " + id, () -> {
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
        return withIndex("delete document " + id, () -> {
            try {
                client.delete()
                        .uri("/{index}/_doc/{id}?version={version}&version_type=external_gte", index, id, version)
                        .retrieve()
                        .toBodilessEntity();
                return true;
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (status == HttpStatus.CONFLICT.value()
                        || (status == HttpStatus.NOT_FOUND.value() && !isIndexNotFound(e))) {
                    return false;
                }
                throw e;
            }
        });
    }

    /** POST /{index}/_search with the given query DSL; returns the raw response. */
    public JsonNode search(JsonNode body) {
        return withIndex("search", () -> json.readTree(client.post()
                .uri("/{index}/_search", index)
                .body(json.write(body))
                .retrieve()
                .body(String.class)));
    }

    /**
     * Runs a request against the index under the read lock. If the index disappeared meanwhile (deleted by hand,
     * Elasticsearch data wiped), it is created again and the request is retried once.
     */
    private <T> T withIndex(String action, Supplier<T> request) {
        for (int attempt = 1; ; attempt++) {
            ensureIndex();
            indexLock.readLock().lock();
            try {
                return call(action, request);
            } catch (IndexMissingException e) {
                if (attempt >= 2) {
                    throw new IllegalStateException("Elasticsearch index '" + index + "' is missing (" + action + ")", e);
                }
                log.warn("Elasticsearch index '{}' disappeared, creating it again ({})", index, action);
                indexReady.set(false);
            } finally {
                indexLock.readLock().unlock();
            }
        }
    }

    private boolean indexExists() {
        try {
            client.head().uri("/{index}", index).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return false;
            }
            throw e;
        }
    }

    /** Our mapping is strict and maps id as keyword; an index auto-created by Elasticsearch is neither. */
    private boolean hasExpectedMapping() {
        JsonNode response = json.readTree(client.get().uri("/{index}/_mapping", index).retrieve().body(String.class));
        // { "<concrete index name>": { "mappings": { "dynamic": "strict", "properties": { "id": { "type": "keyword" } } } } }
        Iterator<JsonNode> indices = response.elements();
        if (!indices.hasNext()) {
            return false;
        }
        JsonNode mappings = indices.next().path("mappings");
        return "strict".equals(mappings.path("dynamic").asText())
                && "keyword".equals(mappings.path("properties").path("id").path("type").asText());
    }

    private void deleteIndex() {
        try {
            client.delete().uri("/{index}", index).retrieve().toBodilessEntity();
            log.warn("Elasticsearch index '{}' deleted", index);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() != HttpStatus.NOT_FOUND.value()) {
                throw e;
            }
        }
    }

    private void createIndex() {
        try {
            client.put().uri("/{index}", index).body(loadIndexDefinition()).retrieve().toBodilessEntity();
            log.info("Elasticsearch index '{}' created from {}", index, INDEX_DEFINITION);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()
                    && e.getResponseBodyAsString().contains("resource_already_exists_exception")) {
                // Another search-service instance created it at the same moment - or Elasticsearch auto-created it.
                if (!hasExpectedMapping()) {
                    throw new IllegalStateException("Elasticsearch index '" + index + "' was created meanwhile with a"
                            + " wrong mapping; it will be checked again on the next request");
                }
                return;
            }
            throw e;
        }
    }

    private static boolean isIndexNotFound(RestClientResponseException e) {
        return e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()
                && e.getResponseBodyAsString().contains("index_not_found_exception");
    }

    private static String loadIndexDefinition() {
        try (InputStream in = new ClassPathResource(INDEX_DEFINITION).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + INDEX_DEFINITION, e);
        }
    }

    /** The index was deleted under our feet (only possible when Elasticsearch may not auto-create it). */
    private static final class IndexMissingException extends RuntimeException {
        IndexMissingException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Connection problems and 5xx become {@link SearchUnavailableException} (retryable); other 4xx are bugs in the
     * request and are rethrown as {@link IllegalStateException} with Elasticsearch's explanation.
     */
    private <T> T call(String action, Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException e) {
            throw new SearchUnavailableException("Elasticsearch unreachable (" + action + "): " + e.getMessage(), e);
        } catch (RestClientResponseException e) {
            if (isIndexNotFound(e)) {
                throw new IndexMissingException(e);
            }
            if (e.getStatusCode().is5xxServerError() || e.getStatusCode().value() == 429) {
                throw new SearchUnavailableException("Elasticsearch error " + e.getStatusCode().value() + " ("
                        + action + "): " + e.getResponseBodyAsString(), e);
            }
            throw new IllegalStateException("Elasticsearch rejected " + action + " (" + e.getStatusCode().value()
                    + "): " + e.getResponseBodyAsString(), e);
        }
    }
}
