package com.mv.bookingservice.infrastructure.config;

import com.mv.bookingservice.infrastructure.messaging.BookingKafkaProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Error handling of the payment-events consumer.
 * <p>
 * Problem: a PAYMENT_SUCCEEDED event means the buyer has paid. If booking-service cannot apply it (database down,
 * optimistic lock storm) and the event is simply skipped, the booking later expires and the buyer loses the seats
 * they paid for. Business mismatches never get here: the handler acknowledges them itself (see
 * HandlePaymentEventService); only technical errors are thrown.
 * <ol>
 *     <li>Blocking retry with exponential backoff (1s, 2s, 4s ... max {@code event-retry-max-interval-ms}) for
 *     {@code event-retry-max-elapsed-ms}. Blocking keeps the order of the events of one booking (same partition).</li>
 *     <li>Still failing: the record is published to {@code payment-events.DLT} (nothing is lost, visible in Kafka UI)
 *     and the partition moves on. Replay it later: POST /api/v1/bookings/admin/payment-events-dlt/replay.</li>
 *     <li>Safety net: the expiry job asks payment-service before expiring an unpaid booking
 *     (BookingMaintenanceService), so even an event still parked in the DLT cannot cost a paid seat.</li>
 * </ol>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(BookingKafkaProperties.class)
public class KafkaConfig {

    /** Created on startup if missing; same partition count as payment-events (3 in the dev compose). */
    @Bean
    public NewTopic paymentEventsDltTopic(BookingKafkaProperties properties) {
        return TopicBuilder.name(properties.paymentEventsDlt()).partitions(3).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate,
                                                 BookingKafkaProperties properties) {
        String dlt = properties.paymentEventsDlt();
        // A record already coming from the DLT (replay) that fails again goes back to the DLT, never to ".DLT.DLT".
        // Partition -1: let Kafka choose (the key, i.e. the bookingId, still keeps one booking on one partition).
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> {
                    log.error("PAYMENT EVENT -> DLT: {}-{}@{} key={} failed after retries: {}",
                            record.topic(), record.partition(), record.offset(), record.key(), ex.getMessage());
                    return new TopicPartition(dlt, -1);
                });

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(properties.eventRetryMaxIntervalMs());
        backOff.setMaxElapsedTime(properties.eventRetryMaxElapsedMs());

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("PAYMENT EVENT retry {} for {}-{}@{}: {}", deliveryAttempt, record.topic(),
                        record.partition(), record.offset(), ex.getMessage()));
        return handler;
    }
}
