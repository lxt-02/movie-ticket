package com.mv.bookingservice.infrastructure.messaging;

import com.mv.bookingservice.application.port.out.OutboxPort;
import com.mv.bookingservice.domain.model.outbox.OutboxEvent;
import com.mv.bookingservice.infrastructure.config.JsonCodec;
import com.mv.bookingservice.infrastructure.config.KafkaConfig;
import com.mv.bookingservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.mv.bookingservice.infrastructure.persistence.entity.OutboxStatus;
import com.mv.bookingservice.infrastructure.persistence.repository.OutboxEventJpaRepository;
import jakarta.persistence.EntityManager;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Outbox + Kafka under failure: broker down, wrong topic, no permission, payload that cannot become a message.
 * <p>
 * Runs on H2 by default. Against a real PostgreSQL (same Flyway migrations as production), set
 * {@code OUTBOX_IT_JDBC_URL} (e.g. jdbc:postgresql://localhost:5433/booking_it_db, an empty database) and optionally
 * {@code OUTBOX_IT_DB_USER} / {@code OUTBOX_IT_DB_PASSWORD} (default postgres / 12345).
 * <p>
 * Kafka: the "broker down" case uses a real KafkaProducer with the producer settings of
 * {@code src/main/resources/application.yaml}, pointed at a closed port. The other cases use Kafka's
 * {@link MockProducer} plus fault injection (the error a real broker would return).
 */
@SpringBootTest
class OutboxKafkaFailureScenariosTest {

    private static final String TOPIC = "payment-commands";

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
        registry.add("spring.jpa.database-platform", () -> "");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    @Autowired
    private OutboxEventJpaRepository repository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OutboxPort outboxPort;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-02T03:00:00Z"));
    private final BookingKafkaProperties properties =
            new BookingKafkaProperties(TOPIC, "payment-events", 50, 15_000L, 10_000L, 60_000L,
                    2_000L, 60_000L, 10, false);

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS outbox_relay_lock (name VARCHAR(50) PRIMARY KEY)");
        jdbcTemplate.update("INSERT INTO outbox_relay_lock (name) SELECT 'booking-outbox' "
                + "WHERE NOT EXISTS (SELECT 1 FROM outbox_relay_lock WHERE name = 'booking-outbox')");
        repository.deleteAll();
    }

    // ------------------------------------------------------------------ normal flow

    @Test
    @DisplayName("1. Bình thường: command lên đúng topic, key = bookingId, đúng thứ tự, dòng outbox PUBLISHED")
    void normalFlowPublishesInOrder() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID createA = row(a, "CREATE_PAYMENT", 0);
        UUID createB = row(b, "CREATE_PAYMENT", 1);
        UUID cancelA = row(a, "CANCEL_PAYMENT", 2);
        MockProducer<String, String> producer = mockProducer(new StringSerializer());

        relay(new KafkaTemplate<>(() -> producer)).relay();

        assertThat(producer.history()).extracting(ProducerRecord::topic).containsOnly(TOPIC);
        assertThat(producer.history()).extracting(ProducerRecord::key)
                .containsExactly(a.toString(), b.toString(), a.toString());
        assertThat(producer.history().get(0).value()).contains(createA.toString());
        assertThat(producer.history().get(2).value()).contains(cancelA.toString());
        assertThat(List.of(createA, createB, cancelA)).allSatisfy(id -> {
            OutboxEventEntity e = repository.findById(id).orElseThrow();
            assertThat(e.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(e.getPublishedAt()).isNotNull();
        });
    }

    // ------------------------------------------------------------------ broker down

    @Test
    @DisplayName("2. Broker tắt: không mất message, relay bỏ cuộc sau ~5s (không phải 60s), back-off, gửi lại khi broker sống")
    void brokerDownKeepsMessageAndRetriesWhenBack() {
        UUID booking = UUID.randomUUID();
        UUID id = row(booking, "CREATE_PAYMENT", 0);
        Map<String, Object> config = shippedProducerConfig();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:1"); // nothing listens: broker "down"
        DefaultKafkaProducerFactory<String, String> realProducer = new DefaultKafkaProducerFactory<>(config);
        BookingOutboxRelay relay = relay(new KafkaTemplate<>(realProducer));
        try {
            long started = System.nanoTime();
            relay.relay();
            Duration took = Duration.ofNanos(System.nanoTime() - started);

            OutboxEventEntity row = repository.findById(id).orElseThrow();
            assertThat(took).isLessThan(Duration.ofSeconds(10));
            assertThat(row.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(row.getPublishedAt()).isNull();
            assertThat(row.getRetryCount()).isEqualTo(1);
            assertThat(row.getLastError()).contains("TimeoutException");
            assertThat(row.getNextRetryAt()).isEqualTo(clock.instant().plusSeconds(2));
            assertThat(relay.pausedUntil()).isEqualTo(row.getNextRetryAt());
            System.out.printf("SCENARIO broker-down: relay gave up after %d ms, row PENDING, last_error=%s%n",
                    took.toMillis(), row.getLastError());

            // Still paused: the relay does not hammer the dead broker.
            long again = System.nanoTime();
            relay.relay();
            assertThat(Duration.ofNanos(System.nanoTime() - again)).isLessThan(Duration.ofMillis(500));
        } finally {
            realProducer.destroy();
        }

        // Broker back (2s later): the same row is delivered, nothing was lost.
        clock.advance(Duration.ofSeconds(2));
        MockProducer<String, String> producer = mockProducer(new StringSerializer());
        relay(new KafkaTemplate<>(() -> producer)).relay();
        assertThat(producer.history()).hasSize(1);
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("2b. Lỗi tạm thời liên tiếp: back-off tăng dần 2s, 4s, 8s ... tối đa 60s")
    void backOffGrows() {
        BookingOutboxRelay relay = relay(new KafkaTemplate<>(() -> mockProducer(new StringSerializer())));
        assertThat(relay.backOff(1)).isEqualTo(Duration.ofSeconds(2));
        assertThat(relay.backOff(2)).isEqualTo(Duration.ofSeconds(4));
        assertThat(relay.backOff(3)).isEqualTo(Duration.ofSeconds(8));
        assertThat(relay.backOff(10)).isEqualTo(Duration.ofSeconds(60));
    }

    // ------------------------------------------------------------------ wrong topic

    @Test
    @DisplayName("3. Sai tên topic (broker không tự tạo topic): dòng giữ PENDING với lỗi rõ ràng, không 'gửi thành công' vào topic ma")
    void wrongTopicIsNotSilentlyDelivered() {
        UUID id = row(UUID.randomUUID(), "CREATE_PAYMENT", 0);
        BookingKafkaProperties typo = new BookingKafkaProperties("payment-comands", "payment-events", 50, 15_000L,
                10_000L, 60_000L, 2_000L, 60_000L, 10, false);
        // What KafkaTemplate throws when auto.create.topics.enable=false and the topic does not exist.
        KafkaTemplate<String, String> template = failing(topic -> new org.apache.kafka.common.KafkaException(
                "Send failed", new TimeoutException("Topic " + topic + " not present in metadata after 5000 ms.")));

        new BookingOutboxRelay(repository, template, typo, transactionTemplate, entityManager, clock).relay();

        OutboxEventEntity row = repository.findById(id).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(row.getPublishedAt()).isNull();
        assertThat(row.getLastError()).contains("payment-comands").contains("not present in metadata");
        System.out.println("SCENARIO wrong-topic: row PENDING, last_error=" + row.getLastError());
    }

    @Test
    @DisplayName("3b. Kiểm tra topic lúc khởi động không chặn / không ném lỗi khi broker tắt")
    void topicVerifierToleratesBrokerDown() {
        org.springframework.kafka.core.KafkaAdmin admin = new org.springframework.kafka.core.KafkaAdmin(
                Map.of("bootstrap.servers", "127.0.0.1:1"));
        long started = System.nanoTime();
        Set<String> missing = new KafkaTopicVerifier(admin, properties).verify(List.of(TOPIC));
        assertThat(missing).isEmpty();
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(10));
    }

    // ------------------------------------------------------------------ no permission

    @Test
    @DisplayName("4. Không có quyền publish: dòng FAILED, booking khác vẫn đi, lệnh sau của cùng booking chờ; requeue sau khi cấp quyền")
    void permissionDeniedDoesNotBlockOtherBookings() {
        UUID denied = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID createDenied = row(denied, "CREATE_PAYMENT", 0);
        UUID createOther = row(other, "CREATE_PAYMENT", 1);
        UUID cancelDenied = row(denied, "CANCEL_PAYMENT", 2);
        MockProducer<String, String> producer = mockProducer(new StringSerializer());
        Set<String> aclDenied = new java.util.HashSet<>(Set.of(denied.toString()));
        KafkaTemplate<String, String> template = new KafkaTemplate<>(() -> producer) {
            @Override
            public CompletableFuture<SendResult<String, String>> send(String topic, String key, String value) {
                if (aclDenied.contains(key)) {
                    return CompletableFuture.failedFuture(new TopicAuthorizationException(Set.of(topic)));
                }
                return super.send(topic, key, value);
            }
        };
        BookingOutboxRelay relay = relay(template);

        relay.relay();

        assertThat(repository.findById(createDenied).orElseThrow().getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(repository.findById(createDenied).orElseThrow().getLastError())
                .startsWith("TopicAuthorizationException");
        assertThat(repository.findById(createOther).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(repository.findById(cancelDenied).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(producer.history()).extracting(ProducerRecord::key).containsExactly(other.toString());
        assertThat(repository.countHeldBehindFailed()).isEqualTo(1);

        relay.relay(); // the CANCEL of the denied booking must not overtake its CREATE
        assertThat(producer.history()).hasSize(1);

        // Operator grants the ACL, then requeues: CREATE then CANCEL, in order.
        aclDenied.clear();
        transactionTemplate.executeWithoutResult(s -> assertThat(repository.requeueFailed()).isEqualTo(1));
        relay.relay();
        assertThat(producer.history()).extracting(ProducerRecord::key)
                .containsExactly(other.toString(), denied.toString(), denied.toString());
        assertThat(producer.history().get(1).value()).contains(createDenied.toString());
        assertThat(producer.history().get(2).value()).contains(cancelDenied.toString());
        assertThat(repository.countByStatus(OutboxStatus.PUBLISHED)).isEqualTo(3);
    }

    // ------------------------------------------------------------------ payload -> message

    @Test
    @DisplayName("5a. Lỗi chuyển payload thành message (serializer Kafka): dòng FAILED, không chặn dòng khác")
    void serializerErrorIsPermanent() {
        UUID bad = row(UUID.randomUUID(), "CREATE_PAYMENT", 0);
        UUID good = row(UUID.randomUUID(), "CREATE_PAYMENT", 1);
        Serializer<String> serializer = (topic, value) -> {
            if (value.contains(bad.toString())) {
                throw new SerializationException("Cannot serialize value for topic " + topic);
            }
            return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        };
        MockProducer<String, String> producer = mockProducer(serializer);

        relay(new KafkaTemplate<>(() -> producer)).relay();

        assertThat(repository.findById(bad).orElseThrow().getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(repository.findById(bad).orElseThrow().getLastError()).startsWith("SerializationException");
        assertThat(repository.findById(good).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("5b. Lỗi chuyển object thành JSON khi ghi outbox: cả transaction nghiệp vụ rollback (không có booking mồ côi)")
    void jsonErrorRollsBackTheBusinessTransaction() {
        UUID booking = UUID.randomUUID();
        JsonCodec broken = new JsonCodec() {
            @Override
            public String write(Object value) {
                throw new IllegalStateException("Cannot serialize " + value.getClass().getSimpleName());
            }
        };
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(s -> {
            // Something written earlier in the same business transaction...
            outboxPort.save(OutboxEvent.create(booking, "CREATE_PAYMENT", "{\"step\":\"earlier write\"}"));
            // ...then the command payload cannot be turned into JSON.
            outboxPort.save(OutboxEvent.create(booking, "CANCEL_PAYMENT", broken.write(new Object())));
        })).isInstanceOf(IllegalStateException.class);

        assertThat(repository.findAll()).noneMatch(e -> e.getAggregateId().equals(booking));
    }

    @Test
    @DisplayName("5c. Loại event không có topic: FAILED ngay (trước đây bị bỏ qua âm thầm, còn lệnh sau vượt lên)")
    void unknownEventTypeFails() {
        UUID booking = UUID.randomUUID();
        UUID unknown = row(booking, "REFUND_PAYMENT", 0);
        UUID later = row(booking, "CANCEL_PAYMENT", 1);
        MockProducer<String, String> producer = mockProducer(new StringSerializer());

        relay(new KafkaTemplate<>(() -> producer)).relay();

        assertThat(repository.findById(unknown).orElseThrow().getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(repository.findById(later).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(producer.history()).isEmpty();
    }

    @Test
    @DisplayName("5d. Consumer nhận message hỏng: đi thẳng vào DLT (không retry 60s, không bị vứt bỏ)")
    void malformedConsumedRecordGoesStraightToDlt() {
        MockProducer<String, String> producer = mockProducer(new StringSerializer());
        BookingKafkaProperties fastRetry = new BookingKafkaProperties(TOPIC, "payment-events", 50, 15_000L,
                10_000L, 60_000L, 2_000L, 60_000L, 10, false);
        DefaultErrorHandler handler = new KafkaConfig().kafkaErrorHandler(new KafkaTemplate<>(() -> producer), fastRetry);
        PaymentEventListener listener = new PaymentEventListener(new JsonCodec(), command -> {
            throw new AssertionError("must not be called");
        }, new PaymentEventChaos(false));
        ConsumerRecord<String, String> record = new ConsumerRecord<>("payment-events", 1, 42L, "booking-1", "{not json");

        Exception error = null;
        try {
            listener.onMessage(record);
        } catch (NonRetryableMessageException e) {
            error = e;
        }
        assertThat(error).isNotNull();

        long started = System.nanoTime();
        boolean recovered = handler.handleOne(error, record, mock(Consumer.class), mock(MessageListenerContainer.class));

        assertThat(recovered).isTrue();
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(2));
        assertThat(producer.history()).singleElement().satisfies(dlt -> {
            assertThat(dlt.topic()).isEqualTo("payment-events.DLT");
            assertThat(dlt.key()).isEqualTo("booking-1");
            assertThat(dlt.value()).isEqualTo("{not json");
        });
    }

    @Test
    @DisplayName("5e. Lỗi kỹ thuật lúc xử lý (DB sập) KHÔNG vào DLT ngay: được retry trước")
    void technicalErrorIsRetriedFirst() {
        MockProducer<String, String> producer = mockProducer(new StringSerializer());
        DefaultErrorHandler handler = new KafkaConfig().kafkaErrorHandler(new KafkaTemplate<>(() -> producer), properties);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("payment-events", 1, 43L, "booking-2", "{}");

        boolean recovered = handler.handleOne(new IllegalStateException("database down"), record,
                mock(Consumer.class), mock(MessageListenerContainer.class));

        assertThat(recovered).isFalse();
        assertThat(producer.history()).isEmpty();
    }

    // ------------------------------------------------------------------ helpers

    private UUID row(UUID bookingId, String type, int secondsAfterStart) {
        UUID id = UUID.randomUUID();
        repository.save(OutboxEventEntity.builder()
                .id(id)
                .aggregateId(bookingId)
                .eventType(type)
                .payload("{\"commandId\":\"" + id + "\",\"commandType\":\"" + type + "\"}")
                .createdAt(clock.instant().minusSeconds(60).plusSeconds(secondsAfterStart))
                .build());
        return id;
    }

    private BookingOutboxRelay relay(KafkaTemplate<String, String> template) {
        return new BookingOutboxRelay(repository, template, properties, transactionTemplate, entityManager, clock);
    }

    /** In-memory producer standing in for a healthy broker (KafkaTemplate closes it after each send: ignored). */
    private static MockProducer<String, String> mockProducer(Serializer<String> valueSerializer) {
        return new MockProducer<>(true, null, new StringSerializer(), valueSerializer) {
            @Override
            public void close(Duration timeout) {
            }
        };
    }

    private static KafkaTemplate<String, String> failing(Function<String, RuntimeException> error) {
        MockProducer<String, String> unused = mockProducer(new StringSerializer());
        return new KafkaTemplate<>(() -> unused) {
            @Override
            public CompletableFuture<SendResult<String, String>> send(String topic, String key, String value) {
                throw error.apply(topic);
            }
        };
    }

    /** spring.kafka.producer.* of the real application.yaml (the test application.yaml replaces it). */
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

    /** Clock the test moves forward (back-off without sleeping). */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
