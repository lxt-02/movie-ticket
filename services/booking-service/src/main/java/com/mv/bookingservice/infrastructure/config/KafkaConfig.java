package com.mv.bookingservice.infrastructure.config;

import com.mv.bookingservice.infrastructure.messaging.BookingKafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableConfigurationProperties(BookingKafkaProperties.class)
public class KafkaConfig {

    /**
     * Technical errors while applying a payment event (database, Showtime down) are retried 5 times,
     * 5 seconds apart, then logged and skipped.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(5000L, 5L));
    }
}
