package com.mv.paymentservice.infrastructure.config;

import com.mv.paymentservice.application.dto.PaymentDefaults;
import com.mv.paymentservice.infrastructure.messaging.KafkaTopicsProperties;
import com.mv.paymentservice.infrastructure.messaging.OutboxRelayProperties;
import com.mv.paymentservice.infrastructure.payos.PayosProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties({PayosProperties.class, KafkaTopicsProperties.class, OutboxRelayProperties.class})
public class PaymentConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public PaymentDefaults paymentDefaults(PayosProperties payosProperties,
                                           @Value("${payment.expiration-batch-size:100}") int expirationBatchSize,
                                           @Value("${payment.reconcile.min-age-seconds:60}") long reconcileMinAgeSeconds,
                                           @Value("${payment.reconcile.batch-size:50}") int reconcileBatchSize) {
        return new PaymentDefaults(
                Duration.ofMinutes(payosProperties.paymentExpirationMinutes()),
                expirationBatchSize > 0 ? expirationBatchSize : 100,
                Duration.ofSeconds(reconcileMinAgeSeconds > 0 ? reconcileMinAgeSeconds : 60),
                reconcileBatchSize > 0 ? reconcileBatchSize : 50
        );
    }
}
