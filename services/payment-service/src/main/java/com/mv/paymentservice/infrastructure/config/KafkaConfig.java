package com.mv.paymentservice.infrastructure.config;

import com.mv.paymentservice.infrastructure.messaging.KafkaTopicsProperties;
import com.mv.paymentservice.infrastructure.messaging.NonRetryableMessageException;
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

@Slf4j
@Configuration
public class KafkaConfig {

    /** Created on startup if missing (dev convenience). */
    @Bean
    public NewTopic paymentCommandsTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.paymentCommands()).partitions(topics.partitions()).replicas(1).build();
    }

    @Bean
    public NewTopic paymentEventsTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.paymentEvents()).partitions(topics.partitions()).replicas(1).build();
    }

    @Bean
    public NewTopic paymentCommandsDltTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.paymentCommandsDlt()).partitions(topics.partitions()).replicas(1).build();
    }

    /**
     * Listener errors that escape are technical (database down while handling a command or while recording
     * PAYMENT_CREATE_FAILED). They are retried with exponential backoff (1s, 2s, 4s ... max 10s) for about 60s,
     * blocking the partition so the CREATE / CANCEL of one booking keep their order. Still failing: the command is
     * parked in {@code payment-commands.DLT} instead of being skipped, so a CANCEL_PAYMENT (close the PayOS link)
     * is never silently lost.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate,
                                                 KafkaTopicsProperties topics) {
        String dlt = topics.paymentCommandsDlt();
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> {
                    log.error("PAYMENT COMMAND -> DLT: {}-{}@{} key={} failed after retries: {}",
                            record.topic(), record.partition(), record.offset(), record.key(), ex.getMessage());
                    return new TopicPartition(dlt, -1);
                });

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(10_000L);
        backOff.setMaxElapsedTime(60_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        // Unreadable / invalid command: retrying cannot fix it -> DLT at once.
        handler.addNotRetryableExceptions(NonRetryableMessageException.class);
        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("PAYMENT COMMAND retry {} for {}-{}@{}: {}", deliveryAttempt, record.topic(),
                        record.partition(), record.offset(), ex.getMessage()));
        return handler;
    }
}
