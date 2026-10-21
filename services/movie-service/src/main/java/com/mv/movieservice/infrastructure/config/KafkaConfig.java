package com.mv.movieservice.infrastructure.config;

import com.mv.movieservice.infrastructure.messaging.MovieKafkaProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(MovieKafkaProperties.class)
public class KafkaConfig {

    /** Created on startup if missing (spring.kafka.admin.auto-create). */
    @Bean
    public NewTopic movieEventsTopic(MovieKafkaProperties properties) {
        return TopicBuilder.name(properties.movieEvents()).partitions(properties.partitions()).replicas(1).build();
    }
}
