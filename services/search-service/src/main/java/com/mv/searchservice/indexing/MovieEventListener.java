package com.mv.searchservice.indexing;

import com.mv.searchservice.config.JsonCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes topic movie-events. An exception here is retried with backoff (Elasticsearch down) and finally sent
 * to movie-events.DLT (see KafkaConfig); a malformed message goes to the DLT at once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovieEventListener {

    private final JsonCodec json;
    private final MovieIndexer movieIndexer;

    @KafkaListener(topics = "${search.kafka.movie-events:movie-events}", groupId = "${spring.kafka.consumer.group-id:search-service}")
    public void onMovieEvent(ConsumerRecord<String, String> record) {
        log.debug("Kafka RECEIVE {}-{}@{} key={}", record.topic(), record.partition(), record.offset(), record.key());
        MovieEvent event = json.read(record.value(), MovieEvent.class);
        movieIndexer.apply(event);
    }
}
