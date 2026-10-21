package com.mv.searchservice.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Error handling of the movie-events consumer.
 * <ol>
 *     <li>Elasticsearch down: blocking retry with exponential backoff (1s, 2s, 4s ... max
 *     {@code retry-max-interval-ms}) for {@code retry-max-elapsed-ms}. Blocking keeps the order of the events
 *     of one movie (same partition), and nothing is lost while Elasticsearch restarts.</li>
 *     <li>Still failing, or a malformed message: the record goes to {@code movie-events.DLT} (visible in Kafka UI)
 *     and the partition moves on. A later resync (POST /api/v1/movies/search-sync) repairs the index.</li>
 * </ol>
 */
@Slf4j
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic movieEventsDltTopic(SearchProperties properties) {
        return TopicBuilder.name(properties.kafka().movieEventsDlt()).partitions(3).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate, SearchProperties properties) {
        String dlt = properties.kafka().movieEventsDlt();
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> {
                    log.error("MOVIE EVENT -> DLT: {}-{}@{} key={}: {}", record.topic(), record.partition(),
                            record.offset(), record.key(), ex.getMessage());
                    return new TopicPartition(dlt, -1);
                });
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(properties.kafka().retryMaxIntervalMs());
        backOff.setMaxElapsedTime(properties.kafka().retryMaxElapsedMs());
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        // Retrying cannot fix a malformed message or a request Elasticsearch rejects (4xx).
        handler.addNotRetryableExceptions(IllegalArgumentException.class, IllegalStateException.class);
        handler.setRetryListeners((record, ex, attempt) -> log.warn("MOVIE EVENT retry {} for {}-{}@{}: {}",
                attempt, record.topic(), record.partition(), record.offset(), ex.getMessage()));
        return handler;
    }
}
