package com.mv.paymentservice.infrastructure.config;

import com.mv.paymentservice.infrastructure.messaging.KafkaTopicsProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

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

    /**
     * Listener errors that escape (e.g. database down while recording a failure) are retried 3 times,
     * 2 seconds apart, then logged and skipped.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(2000L, 3L));
    }
}
