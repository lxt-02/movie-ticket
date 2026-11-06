package com.mv.paymentservice.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.mv.paymentservice.api.dto.response.ApiResponse;
import com.mv.paymentservice.api.dto.response.BookingPaymentStatusResponse;
import com.mv.paymentservice.application.command.CreatePaymentCommand;
import com.mv.paymentservice.application.event.PaymentEvent;
import com.mv.paymentservice.application.event.PaymentEventFactory;
import com.mv.paymentservice.application.event.PaymentEventType;
import com.mv.paymentservice.application.port.in.CancelBookingPaymentsUseCase;
import com.mv.paymentservice.application.port.in.CreatePaymentUseCase;
import com.mv.paymentservice.application.port.in.RecordPaymentCreationFailureUseCase;
import com.mv.paymentservice.application.port.out.OutboxMessagePort;
import com.mv.paymentservice.domain.model.outbox.OutboxMessage;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import com.mv.paymentservice.infrastructure.config.JsonCodec;
import com.mv.paymentservice.infrastructure.messaging.KafkaTopicsProperties;
import com.mv.paymentservice.infrastructure.messaging.PaymentCommandListener;
import com.mv.paymentservice.infrastructure.messaging.PaymentCommandMessage;
import com.mv.paymentservice.infrastructure.messaging.PaymentEventOutboxAdapter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Contract with booking-service, checked against the shared samples in {@code contracts/booking-payment/}
 * (booking-service runs the mirror test on the same files):
 * <ul>
 *     <li>what payment RECEIVES (payment-commands) is read from the samples without losing a field;</li>
 *     <li>what payment SENDS (payment-events, paid-status) has exactly the fields of the samples.</li>
 * </ul>
 */
class BookingPaymentContractTest {

    private static final Path CONTRACTS = contractsDir();
    private static final Instant NOW = Instant.parse("2026-11-04T03:00:00Z");

    private final JsonCodec json = new JsonCodec();
    private final KafkaTopicsProperties topics = new KafkaTopicsProperties(null, null, null);

    // ---------------------------------------------------------------- shared constants

    @Test
    @DisplayName("Topic names (defaults and application.yaml) match the contract")
    void topicsMatch() {
        JsonNode contractTopics = sample("contract.json").get("topics");
        Properties yaml = applicationYaml();

        assertThat(topics.paymentCommands()).isEqualTo(contractTopics.get("paymentCommands").asText());
        assertThat(topics.paymentEvents()).isEqualTo(contractTopics.get("paymentEvents").asText());
        assertThat(yaml.getProperty("payment.kafka.payment-commands")).isEqualTo(contractTopics.get("paymentCommands").asText());
        assertThat(yaml.getProperty("payment.kafka.payment-events")).isEqualTo(contractTopics.get("paymentEvents").asText());
    }

    @Test
    @DisplayName("Payment emits exactly the event types / reasons of the contract and knows its command types")
    void eventTypesAndReasonsMatch() {
        JsonNode contract = sample("contract.json");

        assertThat(Arrays.stream(PaymentEventType.values()).map(Enum::name).collect(Collectors.toCollection(TreeSet::new)))
                .isEqualTo(texts(contract.get("eventTypes")));
        assertThat(Set.of(PaymentEventFactory.REASON_PROVIDER_ERROR, PaymentEventFactory.REASON_INVALID_REQUEST))
                .isEqualTo(texts(contract.get("createFailedReasons")));
        assertThat(Set.of(PaymentCommandMessage.CREATE_PAYMENT, PaymentCommandMessage.CANCEL_PAYMENT))
                .isEqualTo(texts(contract.get("commandTypes")));
    }

    // ---------------------------------------------------------------- booking -> payment (payment-commands)

    @Test
    @DisplayName("Every field payment reads from a command exists in the samples")
    void commandPayloadFieldsExistInSamples() {
        Set<String> sampled = new TreeSet<>();
        for (String file : new String[]{"create-payment.json", "cancel-payment.json"}) {
            sample(file).get("payload").fieldNames().forEachRemaining(sampled::add);
        }
        Set<String> readByPayment = Arrays.stream(PaymentCommandMessage.Payload.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(sampled).containsAll(readByPayment);
    }

    @Test
    @DisplayName("CREATE_PAYMENT sample reaches the use case with every field filled")
    void createPaymentIsRead() {
        CreatePaymentUseCase create = mock(CreatePaymentUseCase.class);
        listener(create, mock(CancelBookingPaymentsUseCase.class)).onMessage(record("create-payment.json"));

        ArgumentCaptor<CreatePaymentCommand> captor = ArgumentCaptor.forClass(CreatePaymentCommand.class);
        verify(create).createPayment(captor.capture());
        CreatePaymentCommand command = captor.getValue();
        JsonNode payload = sample("create-payment.json").get("payload");

        assertThat(command.getCommandId()).isNotNull();
        assertThat(command.getBookingId()).hasToString(payload.get("bookingId").asText());
        assertThat(command.getUserId()).hasToString(payload.get("userId").asText());
        assertThat(command.getAmount()).isEqualByComparingTo(payload.get("amount").decimalValue());
        assertThat(command.getCurrency()).isEqualTo(payload.get("currency").asText());
        assertThat(command.getProvider()).isEqualTo(payload.get("provider").asText());
        assertThat(command.getDescription()).isEqualTo(payload.get("description").asText());
        assertThat(command.getExpiredAt()).isEqualTo(Instant.parse(payload.get("expiredAt").asText()));
        assertThat(command.getIdempotencyKey()).isEqualTo(payload.get("idempotencyKey").asText());
    }

    @Test
    @DisplayName("CANCEL_PAYMENT sample reaches the use case with booking id and reason")
    void cancelPaymentIsRead() {
        CancelBookingPaymentsUseCase cancel = mock(CancelBookingPaymentsUseCase.class);
        listener(mock(CreatePaymentUseCase.class), cancel).onMessage(record("cancel-payment.json"));

        JsonNode payload = sample("cancel-payment.json").get("payload");
        verify(cancel).cancelOpenPayments(eq(UUID.fromString(payload.get("bookingId").asText())),
                eq(payload.get("reason").asText()));
    }

    // ---------------------------------------------------------------- payment -> booking (payment-events)

    @Test
    @DisplayName("PAYMENT_LINK_CREATED sent by payment has exactly the fields of payment-link-created.json")
    void linkCreatedMatchesContract() {
        Payment payment = pendingPayment();
        payment.attachPaymentLink("link-1", "https://pay.payos.vn/web/link-1", "qr", NOW);

        assertEventMatches(PaymentEventFactory.linkCreated(payment, NOW), "payment-link-created.json");
    }

    @Test
    @DisplayName("PAYMENT_SUCCEEDED sent by payment has exactly the fields of payment-succeeded.json")
    void succeededMatchesContract() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .bookingId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .status(PaymentStatus.SUCCESS)
                .amount(new BigDecimal("150000"))
                .currency("VND")
                .gatewayProvider(Payment.PROVIDER_PAYOS)
                .gatewayTransactionId("link-1")
                .paidAt(NOW.plusSeconds(180))
                .expiredAt(NOW.plusSeconds(420))
                .build();

        assertEventMatches(PaymentEventFactory.statusChanged(payment, NOW), "payment-succeeded.json");
    }

    @Test
    @DisplayName("PAYMENT_CREATE_FAILED sent by payment has exactly the fields of payment-create-failed.json")
    void createFailedMatchesContract() {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .commandId(UUID.randomUUID())
                .bookingId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("150000"))
                .currency("VND")
                .provider("PAYOS")
                .build();

        assertEventMatches(PaymentEventFactory.createFailed(command, PaymentEventFactory.REASON_PROVIDER_ERROR,
                "PayOS timeout", NOW), "payment-create-failed.json");
    }

    // ---------------------------------------------------------------- payment -> booking (HTTP paid-status)

    @Test
    @DisplayName("paid-status answer has exactly the fields of paid-status.json")
    void paidStatusMatchesContract() {
        ApiResponse<BookingPaymentStatusResponse> response = ApiResponse.success(BookingPaymentStatusResponse.builder()
                .bookingId(UUID.randomUUID())
                .paid(true)
                .paymentId(UUID.randomUUID())
                .amount(new BigDecimal("150000"))
                .currency("VND")
                .paidAt(NOW)
                .build());

        JsonNode produced = json.mapper().valueToTree(response);
        assertThat(paths(produced)).isEqualTo(paths(sample("paid-status.json")));
    }

    // ---------------------------------------------------------------- helpers

    private Payment pendingPayment() {
        return Payment.createPending(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("150000"), "VND",
                "payment:create:PAYOS:x:1", NOW.plusSeconds(420), NOW);
    }

    private void assertEventMatches(PaymentEvent event, String file) {
        OutboxMessagePort port = mock(OutboxMessagePort.class);
        new PaymentEventOutboxAdapter(port, topics, json).append(event);
        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(port).save(captor.capture());
        OutboxMessage message = captor.getValue();

        JsonNode produced = json.readTree(message.getPayload());
        JsonNode expected = sample(file);
        assertThat(paths(produced)).isEqualTo(paths(expected));
        assertThat(produced.get("eventType").asText()).isEqualTo(expected.get("eventType").asText());
        assertThat(message.getTopic()).isEqualTo(topics.paymentEvents());
        assertThat(message.getMessageKey()).isEqualTo(event.bookingId().toString());
    }

    private PaymentCommandListener listener(CreatePaymentUseCase create, CancelBookingPaymentsUseCase cancel) {
        return new PaymentCommandListener(json, create, mock(RecordPaymentCreationFailureUseCase.class), cancel);
    }

    private ConsumerRecord<String, String> record(String file) {
        return new ConsumerRecord<>(topics.paymentCommands(), 0, 0L, sample(file).get("aggregateId").asText(), read(file));
    }

    /** Field paths of a JSON object ("payload.bookingId"), values ignored. */
    private static Set<String> paths(JsonNode node) {
        Set<String> out = new TreeSet<>();
        collect(node, "", out);
        return out;
    }

    private static void collect(JsonNode node, String prefix, Set<String> out) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String path = prefix + field.getKey();
            out.add(path);
            if (field.getValue().isObject()) {
                collect(field.getValue(), path + ".", out);
            }
        }
    }

    private static Set<String> texts(JsonNode array) {
        Set<String> out = new TreeSet<>();
        array.forEach(n -> out.add(n.asText()));
        return out;
    }

    private static Properties applicationYaml() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new FileSystemResource("src/main/resources/application.yaml"));
        return factory.getObject();
    }

    private JsonNode sample(String file) {
        return json.readTree(read(file));
    }

    private static String read(String file) {
        try {
            return Files.readString(CONTRACTS.resolve(file));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read contract sample " + file, e);
        }
    }

    /** contracts/booking-payment at the repository root, found from the module directory. */
    private static Path contractsDir() {
        for (Path dir = Paths.get("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve("contracts").resolve("booking-payment");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("contracts/booking-payment not found above " + Paths.get("").toAbsolutePath());
    }
}
