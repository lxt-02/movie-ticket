package com.mv.searchservice.indexing;

import com.mv.searchservice.config.JsonCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Nhận event phim từ movie-service (topic movie-events) và cập nhật index Elasticsearch.
 * Lỗi ở đây (Elasticsearch sập) được Kafka thử lại có back-off, hết lượt thì sang movie-events.DLT
 * (xem KafkaConfig); message hỏng sang DLT ngay.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovieEventListener {

    private final JsonCodec json;
    private final MovieIndexer movieIndexer;

    @KafkaListener(topics = "${search.kafka.movie-events:movie-events}", groupId = "${spring.kafka.consumer.group-id:search-service}")
    /** JSON → MovieEvent → upsert / xoá document trong index (idempotent theo movieId và version). */
    public void onMovieEvent(ConsumerRecord<String, String> record) {
        log.debug("Kafka RECEIVE {}-{}@{} key={}", record.topic(), record.partition(), record.offset(), record.key());
        MovieEvent event = json.read(record.value(), MovieEvent.class);
        movieIndexer.apply(event);
    }
}
