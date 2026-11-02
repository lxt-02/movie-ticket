package com.mv.bookingservice.infrastructure.messaging;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * On startup, checks that the topics booking-service writes to and reads from exist. A wrong name in
 * {@code booking.kafka.*} (or payment-service not started yet) is reported at once in the log, instead of being
 * discovered when a buyer's payment link never arrives. Runs in the background: a broker that is down does not
 * delay startup (the outbox keeps the commands meanwhile).
 * <p>
 * The topics themselves are created by their owner (payment-service creates payment-commands and payment-events);
 * the broker runs with auto.create.topics.enable=false so a typo is never silently created as a new topic.
 */
@Slf4j
@Component
public class KafkaTopicVerifier {

    private final KafkaAdmin kafkaAdmin;
    private final BookingKafkaProperties properties;

    public KafkaTopicVerifier(KafkaAdmin kafkaAdmin, BookingKafkaProperties properties) {
        this.kafkaAdmin = kafkaAdmin;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verifyOnStartup() {
        if (!Boolean.TRUE.equals(properties.verifyTopics())) {
            return;
        }
        Thread.ofVirtual().name("kafka-topic-verifier").start(() ->
                verify(List.of(properties.paymentCommands(), properties.paymentEvents())));
    }

    /** Returns the topics that do not exist (empty: all fine). Broker unreachable: logs and returns empty. */
    public Set<String> verify(List<String> topics) {
        Map<String, Object> config = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        try (AdminClient admin = AdminClient.create(config)) {
            Set<String> existing = admin.listTopics().names().get(6, TimeUnit.SECONDS);
            Set<String> missing = new java.util.TreeSet<>(topics);
            missing.removeAll(existing);
            if (missing.isEmpty()) {
                log.info("KAFKA topics OK: {}", topics);
            } else {
                log.error("KAFKA topic(s) {} do not exist. Check booking.kafka.* (spelling) and that payment-service "
                        + "(owner of the topics) has started. Outbox rows stay PENDING until the topic exists.", missing);
            }
            return missing;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Set.of();
        } catch (ExecutionException | java.util.concurrent.TimeoutException | RuntimeException e) {
            Throwable cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
            if (cause instanceof UnknownTopicOrPartitionException) {
                return Set.copyOf(topics);
            }
            log.warn("KAFKA topic check skipped, broker not reachable: {}", cause.toString());
            return Set.of();
        }
    }
}
