package com.mv.bookingservice.contract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mv.bookingservice.application.command.PaymentEventCommand;
import com.mv.bookingservice.application.port.in.HandlePaymentEventUseCase;
import com.mv.bookingservice.application.port.out.OutboxMessagePort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.outbox.OutboxMessage;
import com.mv.bookingservice.infrastructure.client.dto.BookingPaymentStatusResponse;
import com.mv.bookingservice.infrastructure.client.dto.RemoteResponse;
import com.mv.bookingservice.infrastructure.config.JsonCodec;
import com.mv.bookingservice.infrastructure.messaging.BookingKafkaProperties;
import com.mv.bookingservice.infrastructure.messaging.PaymentCommandOutboxAdapter;
import com.mv.bookingservice.infrastructure.messaging.PaymentEventChaos;
import com.mv.bookingservice.infrastructure.messaging.PaymentEventListener;
import com.mv.bookingservice.infrastructure.messaging.PaymentEventMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Contract with payment-service, checked against the shared samples in {@code contracts/booking-payment/}
 * (payment-service runs the mirror test on the same files):
 * <ul>
 *     <li>what booking SENDS (payment-commands) has exactly the fields of the sample;</li>
 *     <li>what booking RECEIVES (payment-events, paid-status) is read from the samples without losing a field.</li>
 * </ul>
 */
class BookingPaymentContractTest {

    private static final Path CONTRACTS = contractsDir();
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final JsonCodec json = new JsonCodec();
    private final BookingKafkaProperties kafka = new BookingKafkaProperties(
            null, null, null, null, null, null, null, null, null, null);

    // ---------------------------------------------------------------- shared constants

    @Test
    @DisplayName("Topic names (defaults and application.yaml) match the contract")
    void topicsMatch() {
        JsonNode topics = sample("contract.json").get("topics");
        Properties yaml = applicationYaml();

        assertThat(kafka.paymentCommands()).isEqualTo(topics.get("paymentCommands").asText());
        assertThat(kafka.paymentEvents()).isEqualTo(topics.get("paymentEvents").asText());
        assertThat(yaml.getProperty("booking.kafka.payment-commands")).isEqualTo(topics.get("paymentCommands").asText());
        assertThat(yaml.getProperty("booking.kafka.payment-events")).isEqualTo(topics.get("paymentEvents").asText());
    }

    @Test
    @DisplayName("Booking knows every event type and every PAYMENT_CREATE_FAILED reason of the contract")
    void eventTypesAndReasonsMatch() {
        JsonNode contract = sample("contract.json");

        assertThat(constants(PaymentEventCommand.class, "PAYMENT_"))
                .isEqualTo(texts(contract.get("eventTypes")));
        assertThat(constants(PaymentEventCommand.class, "REASON_"))
                .isEqualTo(texts(contract.get("createFailedReasons")));
        assertThat(Set.of(PaymentCommandOutboxAdapter.CREATE_PAYMENT, PaymentCommandOutboxAdapter.CANCEL_PAYMENT))
                .isEqualTo(texts(contract.get("commandTypes")));
    }

    // ---------------------------------------------------------------- booking -> payment (payment-commands)

    @Test
    @DisplayName("CREATE_PAYMENT sent by booking has exactly the fields of create-payment.json")
    void createPaymentMatchesContract() throws IOException {
        Booking booking = booking();

        OutboxMessage message = sendWith(adapter -> adapter.requestPayment(booking));
        JsonNode produced = MAPPER.readTree(message.getPayload());

        assertThat(paths(produced)).isEqualTo(paths(sample("create-payment.json")));
        assertThat(produced.get("commandType").asText()).isEqualTo("CREATE_PAYMENT");
        assertThat(produced.at("/payload/amount").decimalValue()).isEqualByComparingTo(booking.getTotalAmount());
        assertThat(message.getTopic()).isEqualTo(kafka.paymentCommands());
        assertThat(message.getMessageKey()).isEqualTo(booking.getId().toString());
    }

    @Test
    @DisplayName("CANCEL_PAYMENT sent by booking has exactly the fields of cancel-payment.json")
    void cancelPaymentMatchesContract() throws IOException {
        Booking booking = booking();

        OutboxMessage message = sendWith(adapter -> adapter.cancelPayments(booking, "Booking cancelled"));
        JsonNode produced = MAPPER.readTree(message.getPayload());

        assertThat(paths(produced)).isEqualTo(paths(sample("cancel-payment.json")));
        assertThat(produced.get("commandType").asText()).isEqualTo("CANCEL_PAYMENT");
        assertThat(message.getMessageKey()).isEqualTo(booking.getId().toString());
    }

    // ---------------------------------------------------------------- payment -> booking (payment-events)

    @Test
    @DisplayName("Every field booking reads from a payment event exists in the samples")
    void eventPayloadFieldsExistInSamples() {
        Set<String> sampled = new TreeSet<>();
        for (String file : new String[]{"payment-link-created.json", "payment-succeeded.json", "payment-create-failed.json"}) {
            sample(file).get("payload").fieldNames().forEachRemaining(sampled::add);
        }
        Set<String> readByBooking = Arrays.stream(PaymentEventMessage.Payload.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(sampled).containsAll(readByBooking);
    }

    @Test
    @DisplayName("PAYMENT_SUCCEEDED sample reaches the use case with payment id, amount, currency and paidAt")
    void paymentSucceededIsRead() {
        PaymentEventCommand command = receive("payment-succeeded.json");
        JsonNode payload = sample("payment-succeeded.json").get("payload");

        assertThat(command.getEventType()).isEqualTo(PaymentEventCommand.PAYMENT_SUCCEEDED);
        assertThat(command.getEventId()).isNotNull();
        assertThat(command.getBookingId()).hasToString(payload.get("bookingId").asText());
        assertThat(command.getPaymentId()).hasToString(payload.get("paymentId").asText());
        assertThat(command.getAmount()).isEqualByComparingTo(payload.get("amount").decimalValue());
        assertThat(command.getCurrency()).isEqualTo(payload.get("currency").asText());
        assertThat(command.getPaidAt()).isEqualTo(Instant.parse(payload.get("paidAt").asText()));
    }

    @Test
    @DisplayName("PAYMENT_CREATE_FAILED sample reaches the use case with its reason code")
    void paymentCreateFailedIsRead() {
        PaymentEventCommand command = receive("payment-create-failed.json");

        assertThat(command.getEventType()).isEqualTo(PaymentEventCommand.PAYMENT_CREATE_FAILED);
        assertThat(command.getReasonCode()).isEqualTo(PaymentEventCommand.REASON_PROVIDER_ERROR);
        assertThat(command.getReason()).contains("PayOS timeout");
        assertThat(command.getBookingId()).isNotNull();
    }

    @Test
    @DisplayName("PAYMENT_LINK_CREATED sample is read")
    void paymentLinkCreatedIsRead() {
        PaymentEventCommand command = receive("payment-link-created.json");

        assertThat(command.getEventType()).isEqualTo(PaymentEventCommand.PAYMENT_LINK_CREATED);
        assertThat(command.getPaymentId()).isNotNull();
        assertThat(command.getBookingId()).isNotNull();
    }

    // ---------------------------------------------------------------- payment -> booking (HTTP paid-status)

    @Test
    @DisplayName("paid-status sample is read by the Feign DTO, and the DTO has exactly the sample's fields")
    void paidStatusMatchesContract() throws IOException {
        String body = Files.readString(CONTRACTS.resolve("paid-status.json"));
        RemoteResponse<BookingPaymentStatusResponse> response =
                MAPPER.readValue(body, new TypeReference<RemoteResponse<BookingPaymentStatusResponse>>() { });

        assertThat(response.isSuccess()).isTrue();
        BookingPaymentStatusResponse data = response.getData();
        assertThat(data.isPaid()).isTrue();
        assertThat(data.getBookingId()).isNotNull();
        assertThat(data.getPaymentId()).isNotNull();
        assertThat(data.getAmount()).isNotNull();
        assertThat(data.getCurrency()).isEqualTo("VND");
        assertThat(data.getPaidAt()).isNotNull();

        Set<String> dtoFields = paths(MAPPER.valueToTree(data));
        assertThat(dtoFields).isEqualTo(paths(sample("paid-status.json").get("data")));
    }

    // ---------------------------------------------------------------- helpers

    private Booking booking() {
        return Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BK-0001")
                .userId(UUID.randomUUID())
                .totalAmount(new BigDecimal("150000"))
                .currency("VND")
                .expiresAt(Instant.now().plusSeconds(420))
                .paymentAttempt(1)
                .build();
    }

    private OutboxMessage sendWith(java.util.function.Consumer<PaymentCommandOutboxAdapter> action) {
        OutboxMessagePort port = mock(OutboxMessagePort.class);
        action.accept(new PaymentCommandOutboxAdapter(port, kafka, json));
        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(port).save(captor.capture());
        return captor.getValue();
    }

    private PaymentEventCommand receive(String file) {
        HandlePaymentEventUseCase useCase = mock(HandlePaymentEventUseCase.class);
        PaymentEventListener listener = new PaymentEventListener(json, useCase, new PaymentEventChaos(false));
        String body = read(file);
        listener.onMessage(new ConsumerRecord<>(kafka.paymentEvents(), 0, 0L,
                sample(file).get("aggregateId").asText(), body));
        ArgumentCaptor<PaymentEventCommand> captor = ArgumentCaptor.forClass(PaymentEventCommand.class);
        verify(useCase).handle(captor.capture());
        return captor.getValue();
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

    private static Set<String> constants(Class<?> type, String prefix) {
        Set<String> out = new TreeSet<>();
        for (Field f : type.getDeclaredFields()) {
            int m = f.getModifiers();
            if (Modifier.isStatic(m) && Modifier.isFinal(m) && f.getType() == String.class && f.getName().startsWith(prefix)) {
                try {
                    out.add((String) f.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return out;
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

    private static JsonNode sample(String file) {
        try {
            return MAPPER.readTree(read(file));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot parse " + file, e);
        }
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
