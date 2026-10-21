package com.mv.searchservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * search.* settings.
 *
 * @param elasticsearch connection to Elasticsearch
 * @param kafka         topic and retry policy of the movie-events consumer
 */
@ConfigurationProperties(prefix = "search")
public record SearchProperties(Elasticsearch elasticsearch, Kafka kafka) {

    public SearchProperties {
        if (elasticsearch == null) {
            elasticsearch = new Elasticsearch(null, null, null, null, null, null);
        }
        if (kafka == null) {
            kafka = new Kafka(null, null, null);
        }
    }

    /**
     * @param url              e.g. http://localhost:9200
     * @param username         basic auth user (empty = security disabled, as in the dev docker-compose)
     * @param password         basic auth password
     * @param movieIndex       index name
     * @param connectTimeoutMs TCP connect timeout
     * @param readTimeoutMs    response timeout
     */
    public record Elasticsearch(String url, String username, String password, String movieIndex,
                                Integer connectTimeoutMs, Integer readTimeoutMs) {
        public Elasticsearch {
            if (url == null || url.isBlank()) {
                url = "http://localhost:9200";
            }
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            if (movieIndex == null || movieIndex.isBlank()) {
                movieIndex = "movies";
            }
            if (connectTimeoutMs == null || connectTimeoutMs <= 0) {
                connectTimeoutMs = 3000;
            }
            if (readTimeoutMs == null || readTimeoutMs <= 0) {
                readTimeoutMs = 10_000;
            }
        }

        public boolean hasCredentials() {
            return username != null && !username.isBlank();
        }
    }

    /**
     * @param movieEvents         topic produced by movie-service; failed records go to {@code <movieEvents>.DLT}
     * @param retryMaxIntervalMs  longest pause between two attempts of a failing event (Elasticsearch down)
     * @param retryMaxElapsedMs   an event still failing after this long goes to the dead-letter topic
     */
    public record Kafka(String movieEvents, Long retryMaxIntervalMs, Long retryMaxElapsedMs) {
        public Kafka {
            if (movieEvents == null || movieEvents.isBlank()) {
                movieEvents = "movie-events";
            }
            if (retryMaxIntervalMs == null || retryMaxIntervalMs <= 0) {
                retryMaxIntervalMs = 30_000L;
            }
            if (retryMaxElapsedMs == null || retryMaxElapsedMs <= 0) {
                retryMaxElapsedMs = 300_000L;
            }
        }

        public String movieEventsDlt() {
            return movieEvents + ".DLT";
        }
    }
}
