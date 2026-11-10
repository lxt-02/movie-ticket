package com.mv.paymentservice.infrastructure.messaging;

import com.mv.paymentservice.application.port.in.CancelBookingPaymentsUseCase;
import com.mv.paymentservice.application.port.in.CreatePaymentUseCase;
import com.mv.paymentservice.application.port.in.RecordPaymentCreationFailureUseCase;
import com.mv.paymentservice.infrastructure.config.JsonCodec;
import com.mv.paymentservice.infrastructure.config.KafkaConfig;
import com.mv.paymentservice.infrastructure.persistence.entity.OutboxMessageInfraEntity;
import com.mv.paymentservice.infrastructure.persistence.repository.OutboxMessageJpaRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * payment-service outbox (PAYMENT_* events to payment-events) under failure. H2 by default; real PostgreSQL with
 * {@code OUTBOX_IT_JDBC_URL} (e.g. jdbc:postgresql://localhost:5433/payment_it_db, an empty database),
 * {@code OUTBOX_IT_DB_USER} / {@code OUTBOX_IT_DB_PASSWORD} (default postgres / 12345).
 */
@SpringBootTest
class PaymentOutboxFailureScenariosTest {

    private static final String TOPIC = "payment-events";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("OUTBOX_IT_JDBC_URL");
        if (url == null || url.isBlank()) {
            return;
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", () -> env("OUTBOX_IT_DB_USER", "postgres"));
        registry.add("spring.datasource.password", () -> env("OUTBOX_IT_DB_PASSWORD", "12345"));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    @Autowired
    private OutboxMessageJpaRepository repository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-02T03:00:00Z"));
    private final OutboxRelayProperties properties = new OutboxRelayProperties(50, 10, 15_000L, 5_000L, 120_000L, 300_000L);

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("1. Bình thường: event lên payment-events, key = bookingId, PUBLISHED")
    void normalFlow() {
        UUID booking = UUID.randomUUID();
        UUID id = message(booking, 0);
        MockProducer<String, String> producer = mockProducer(new StringSerializer());

        relay(new KafkaTemplate<>(() -> producer)).relay();

        assertThat(producer.history()).singleElement().satisfies(r -> {
            assertThat(r.topic()).isEqualTo(TOPIC);
            assertThat(r.key()).isEqualTo(booking.toString());
        });
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("2. Broker tắt: một lượt ~5s (trước: 60s x mỗi message), phần còn lại của batch được nhả, không FAILED")
    void brokerDownReleasesTheBatch() {
        UUID first = message(UUID.randomUUID(), 0);
        UUID second = message(UUID.randomUUID(), 1);
        UUID third = message(UUID.randomUUID(), 2);
        Map<String, Object> config = shippedProducerConfig();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:1");
        DefaultKafkaProducerFactory<String, String> realProducer = new DefaultKafkaProducerFactory<>(config);
        try {
            long started = System.nanoTime();
            relay(new KafkaTemplate<>(realProducer)).relay();
            Duration took = Duration.ofNanos(System.nanoTime() - started);
            assertThat(took).isLessThan(Duration.ofSeconds(10));
            System.out.printf("SCENARIO payment broker-down: one run with 3 messages took %d ms%n", took.toMillis());
        } finally {
            realProducer.destroy();
        }
        OutboxMessageInfraEntity attempted = repository.findById(first).orElseThrow();
        assertThat(attempted.getStatus()).isEqualTo("PENDING");
        assertThat(attempted.getRetryCount()).isEqualTo(1);
        assertThat(attempted.getLastError()).contains("TimeoutException");
        assertThat(attempted.getNextRetryAt()).isEqualTo(clock.instant().plusSeconds(5));
        assertThat(List.of(second, third)).allSatisfy(id -> {
            OutboxMessageInfraEntity m = repository.findById(id).orElseThrow();
            assertThat(m.getStatus()).isEqualTo("PENDING");
            assertThat(m.getRetryCount()).isZero();
        });
    }

    @Test
    @DisplayName("2b. Broker tắt rất lâu (15 lần thử): vẫn PENDING, không bao giờ FAILED; back-off tối đa 5 phút; broker sống lại thì gửi")
    void longOutageNeverLosesTheEvent() {
        UUID id = message(UUID.randomUUID(), 0);
        KafkaTemplate<String, String> down = failing(new org.apache.kafka.common.KafkaException("Send failed",
                new TimeoutException("Topic payment-events not present in metadata after 5000 ms.")));
        OutboxRelay relay = relay(down);
        for (int attempt = 1; attempt <= 15; attempt++) {
            relay.relay();
            OutboxMessageInfraEntity m = repository.findById(id).orElseThrow();
            assertThat(m.getStatus()).isEqualTo("PENDING");
            clock.set(m.getNextRetryAt());
        }
        OutboxMessageInfraEntity afterOutage = repository.findById(id).orElseThrow();
        assertThat(afterOutage.getRetryCount()).isEqualTo(15);
        assertThat(relay.backOff(15)).isEqualTo(Duration.ofMinutes(5));

        MockProducer<String, String> producer = mockProducer(new StringSerializer());
        relay(new KafkaTemplate<>(() -> producer)).relay();
        assertThat(producer.history()).hasSize(1);
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("3. Không có quyền publish: FAILED ngay ở lần đầu (không lãng phí 10 lần), message khác vẫn đi; requeue sau khi cấp quyền")
    void permissionDenied() {
        UUID denied = UUID.randomUUID();
        UUID deniedId = message(denied, 0);
        UUID okId = message(UUID.randomUUID(), 1);
        MockProducer<String, String> producer = mockProducer(new StringSerializer());
        Set<String> acl = new HashSet<>(Set.of(denied.toString()));
        KafkaTemplate<String, String> template = new KafkaTemplate<>(() -> producer) {
            @Override
            public CompletableFuture<SendResult<String, String>> send(String topic, String key, String value) {
                return acl.contains(key)
                        ? CompletableFuture.failedFuture(new TopicAuthorizationException(Set.of(topic)))
                        : super.send(topic, key, value);
            }
        };
        OutboxRelay relay = relay(template);

        relay.relay();

        OutboxMessageInfraEntity failed = repository.findById(deniedId).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getLastError()).startsWith("TopicAuthorizationException");
        assertThat(repository.findById(okId).orElseThrow().getStatus()).isEqualTo("PUBLISHED");

        relay.relay();
        assertThat(producer.history()).hasSize(1); // FAILED is not retried automatically

        acl.clear();
        transactionTemplate.executeWithoutResult(s -> assertThat(repository.requeueFailed()).isEqualTo(1));
        relay.relay();
        assertThat(producer.history()).hasSize(2);
        assertThat(repository.findById(deniedId).orElseThrow().getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("4. Lỗi serializer khi chuyển payload thành message: FAILED, không retry vô ích")
    void serializerError() {
        UUID id = message(UUID.randomUUID(), 0);
        Serializer<String> broken = (topic, value) -> {
            throw new SerializationException("Cannot serialize value for " + topic);
        };
        relay(new KafkaTemplate<>(() -> mockProducer(broken))).relay();
        OutboxMessageInfraEntity m = repository.findById(id).orElseThrow();
        assertThat(m.getStatus()).isEqualTo("FAILED");
        assertThat(m.getLastError()).startsWith("SerializationException");
    }

    @Test
    @DisplayName("5. Command hỏng / không rõ loại: vào payment-commands.DLT ngay, không gọi nghiệp vụ")
    void invalidCommandsGoStraightToDlt() {
        CreatePaymentUseCase create = mock(CreatePaymentUseCase.class);
        RecordPaymentCreationFailureUseCase recordFailure = mock(RecordPaymentCreationFailureUseCase.class);
        CancelBookingPaymentsUseCase cancel = mock(CancelBookingPaymentsUseCase.class);
        PaymentCommandListener listener = new PaymentCommandListener(new JsonCodec(), create, recordFailure, cancel);
        MockProducer<String, String> producer = mockProducer(new StringSerializer());
        DefaultErrorHandler handler = new KafkaConfig().kafkaErrorHandler(new KafkaTemplate<>(() -> producer),
                new KafkaTopicsProperties("payment-commands", "payment-events", 3));

        List<String> bad = List.of("{oops", "{\"commandId\":\"" + UUID.randomUUID() + "\",\"commandType\":\"REFUND\"}");
        for (int i = 0; i < bad.size(); i++) {
            ConsumerRecord<String, String> record = new ConsumerRecord<>("payment-commands", 0, i, "k" + i, bad.get(i));
            Exception error = catchThrown(() -> listener.onMessage(record));
            assertThat(error).isInstanceOf(NonRetryableMessageException.class);
            assertThat(handler.handleOne(error, record, mock(Consumer.class), mock(MessageListenerContainer.class)))
                    .isTrue();
        }
        assertThat(producer.history()).extracting(ProducerRecord::topic)
                .containsExactly("payment-commands.DLT", "payment-commands.DLT");
        assertThat(producer.history()).extracting(ProducerRecord::value).containsExactlyElementsOf(bad);
        verifyNoInteractions(create, recordFailure, cancel);
    }

    // ------------------------------------------------------------------ helpers

    private static Exception catchThrown(Runnable action) {
        try {
            action.run();
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    private UUID message(UUID bookingId, int secondsAfterStart) {
        UUID id = UUID.randomUUID();
        repository.save(OutboxMessageInfraEntity.builder()
                .id(id).aggregateType("BOOKING").aggregateId(bookingId)
                .eventType("PAYMENT_SUCCEEDED").topic(TOPIC).messageKey(bookingId.toString())
                .payload("{\"eventId\":\"" + id + "\"}")
                .status("PENDING").retryCount(0)
                .createdAt(clock.instant().minusSeconds(60).plusSeconds(secondsAfterStart))
                .build());
        return id;
    }

    private OutboxRelay relay(KafkaTemplate<String, String> template) {
        return new OutboxRelay(repository, template, transactionTemplate, properties, clock);
    }

    private static MockProducer<String, String> mockProducer(Serializer<String> valueSerializer) {
        return new MockProducer<>(true, null, new StringSerializer(), valueSerializer) {
            @Override
            public void close(Duration timeout) {
            }
        };
    }

    private static KafkaTemplate<String, String> failing(RuntimeException error) {
        MockProducer<String, String> unused = mockProducer(new StringSerializer());
        return new KafkaTemplate<>(() -> unused) {
            @Override
            public CompletableFuture<SendResult<String, String>> send(String topic, String key, String value) {
                throw error;
            }
        };
    }

    private static Map<String, Object> shippedProducerConfig() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("src/main/resources/application.yaml"));
        Properties props = yaml.getObject();
        Map<String, Object> config = new HashMap<>();
        String prefix = "spring.kafka.producer.properties";
        // keySet, not stringPropertyNames: YAML numbers and booleans are not Strings.
        props.keySet().stream().map(String::valueOf).filter(k -> k.startsWith(prefix))
                .forEach(k -> config.put(k.substring(prefix.length()).replaceAll("^[.\\[]|]$", ""),
                        String.valueOf(props.get(k))));
        config.put(ProducerConfig.ACKS_CONFIG, String.valueOf(props.get("spring.kafka.producer.acks")));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        assertThat(config).containsKeys("max.block.ms", "delivery.timeout.ms");
        return config;
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void set(Instant instant) {
            now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
