package com.mv.movieservice.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * movie.kafka.* settings.
 *
 * @param movieEvents      topic for MOVIE_* events (consumed by search-service)
 * @param partitions       partitions of the topic when movie-service creates it
 * @param relayBatchSize   outbox rows published per relay run
 * @param publishTimeoutMs wait for the broker ack
 */
@ConfigurationProperties(prefix = "movie.kafka")
public record MovieKafkaProperties(
        String movieEvents,
        Integer partitions,
        Integer relayBatchSize,
        Long publishTimeoutMs
) {

    public MovieKafkaProperties {
        if (movieEvents == null || movieEvents.isBlank()) {
            movieEvents = "movie-events";
        }
        if (partitions == null || partitions <= 0) {
            partitions = 3;
        }
        if (relayBatchSize == null || relayBatchSize <= 0) {
            relayBatchSize = 100;
        }
        if (publishTimeoutMs == null || publishTimeoutMs <= 0) {
            publishTimeoutMs = 5000L;
        }
    }
}
