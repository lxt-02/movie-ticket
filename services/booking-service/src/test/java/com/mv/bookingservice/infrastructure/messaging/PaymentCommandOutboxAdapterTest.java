package com.mv.bookingservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.mv.bookingservice.application.port.out.OutboxMessagePort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.outbox.OutboxMessage;
import com.mv.bookingservice.domain.model.outbox.OutboxMessageStatus;
import com.mv.bookingservice.infrastructure.config.JsonCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * What booking-service asks payment-service to do when a customer places an order (CREATE_PAYMENT) and when the
 * order is cancelled or expires (CANCEL_PAYMENT). The command is stored in outbox_messages, so these tests check
 * the stored row: topic, key, status and the VALUES inside the payload.
 * (BookingPaymentContractTest checks the field names against the shared samples.)
 */
class PaymentCommandOutboxAdapterTest {

    private static final Instant HOLD_DEADLINE = Instant.parse("2026-11-06T10:07:00Z");

    private final JsonCodec json = new JsonCodec();
    private OutboxMessagePort outboxPort;
    private PaymentCommandOutboxAdapter adapter;

    @BeforeEach
    void setUp() {
        outboxPort = mock(OutboxMessagePort.class);
        // all null = default topic names (payment-commands / payment-events)
        BookingKafkaProperties kafka = new BookingKafkaProperties(null, null, null, null, null, null, null, null, null, null);
        adapter = new PaymentCommandOutboxAdapter(outboxPort, kafka, json);
    }

    private Booking booking(int attempt) {
        return Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BK-0001")
                .userId(UUID.randomUUID())
                .totalAmount(new BigDecimal("150000"))
                .currency("VND")
                .expiresAt(HOLD_DEADLINE)
                .paymentAttempt(attempt)
                .build();
    }

    private OutboxMessage saved() {
        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxPort).save(captor.capture());
        verifyNoMoreInteractions(outboxPort);
        return captor.getValue();
    }

    private JsonNode body(OutboxMessage message) {
        return json.read(message.getPayload(), JsonNode.class);
    }

    @Test
    @DisplayName("order: CREATE_PAYMENT is stored PENDING on payment-commands, keyed by bookingId")
    void orderStoresCreatePaymentForTheRelay() {
        Booking booking = booking(1);

        adapter.requestPayment(booking);

        OutboxMessage message = saved();
        assertThat(message.getEventType()).isEqualTo("CREATE_PAYMENT");
        assertThat(message.getAggregateType()).isEqualTo("BOOKING");
        assertThat(message.getAggregateId()).isEqualTo(booking.getId());
        assertThat(message.getTopic()).isEqualTo("payment-commands");
        // same key for every command of a booking: one partition, so CREATE and CANCEL keep their order
        assertThat(message.getMessageKey()).isEqualTo(booking.getId().toString());
        assertThat(message.getStatus()).isEqualTo(OutboxMessageStatus.PENDING);
        assertThat(message.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("order: payload carries the amount, the buyer and the payment deadline of the booking")
    void orderPayloadHasBookingValues() {
        Booking booking = booking(1);

        adapter.requestPayment(booking);

        JsonNode command = body(saved());
        assertThat(command.get("commandType").asText()).isEqualTo("CREATE_PAYMENT");
        assertThat(command.get("aggregateId").asText()).isEqualTo(booking.getId().toString());
        assertThat(command.get("commandId").asText()).isNotBlank();

        JsonNode payload = command.get("payload");
        assertThat(payload.get("bookingId").asText()).isEqualTo(booking.getId().toString());
        assertThat(payload.get("userId").asText()).isEqualTo(booking.getUserId().toString());
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("150000");
        assertThat(payload.get("currency").asText()).isEqualTo("VND");
        assertThat(payload.get("provider").asText()).isEqualTo("PAYOS");
        assertThat(payload.get("description").asText()).isEqualTo("Booking BK-0001");
        // the PayOS link must not outlive the seat hold
        assertThat(Instant.parse(payload.get("expiredAt").asText())).isEqualTo(HOLD_DEADLINE);
    }

    @Test
    @DisplayName("order: idempotency key is payment:create:PAYOS:{bookingId}:{attempt}")
    void orderIdempotencyKeyContainsAttempt() {
        Booking booking = booking(1);

        adapter.requestPayment(booking);

        String key = body(saved()).get("payload").get("idempotencyKey").asText();
        assertThat(key).isEqualTo("payment:create:PAYOS:" + booking.getId() + ":1");
    }

    @Test
    @DisplayName("pay again: the next attempt sends a NEW key, so payment-service replaces the old link")
    void payAgainUsesNewKey() {
        Booking booking = booking(1);
        adapter.requestPayment(booking);
        String firstKey = body(captureLast()).get("payload").get("idempotencyKey").asText();

        Booking sameBookingSecondAttempt = Booking.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .userId(booking.getUserId())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .expiresAt(booking.getExpiresAt())
                .paymentAttempt(2)
                .build();
        adapter.requestPayment(sameBookingSecondAttempt);
        String secondKey = body(captureLast()).get("payload").get("idempotencyKey").asText();

        assertThat(secondKey).isNotEqualTo(firstKey);
        assertThat(secondKey).isEqualTo("payment:create:PAYOS:" + booking.getId() + ":2");
    }

    @Test
    @DisplayName("redelivery: the same attempt always gives the same key, so payment-service returns the same payment")
    void sameAttemptGivesSameKeyButNewCommandId() {
        Booking booking = booking(1);

        adapter.requestPayment(booking);
        adapter.requestPayment(booking);

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxPort, org.mockito.Mockito.times(2)).save(captor.capture());
        JsonNode first = body(captor.getAllValues().get(0));
        JsonNode second = body(captor.getAllValues().get(1));

        assertThat(second.get("payload").get("idempotencyKey").asText())
                .isEqualTo(first.get("payload").get("idempotencyKey").asText());
        assertThat(second.get("commandId").asText()).isNotEqualTo(first.get("commandId").asText());
    }

    @Test
    @DisplayName("order without a payment attempt number counts as attempt 1")
    void missingAttemptDefaultsToOne() {
        Booking booking = booking(1);
        Booking noAttempt = Booking.builder()
                .id(booking.getId())
                .bookingCode("BK-0002")
                .userId(booking.getUserId())
                .totalAmount(booking.getTotalAmount())
                .currency("VND")
                .expiresAt(HOLD_DEADLINE)
                .build();

        adapter.requestPayment(noAttempt);

        String key = body(saved()).get("payload").get("idempotencyKey").asText();
        assertThat(key).endsWith(":1");
    }

    @Test
    @DisplayName("cancel or expire: CANCEL_PAYMENT carries the booking and the reason, same topic and key")
    void cancelStoresCancelPaymentWithReason() {
        Booking booking = booking(1);

        adapter.cancelPayments(booking, "Booking cancelled by user");

        OutboxMessage message = saved();
        assertThat(message.getEventType()).isEqualTo("CANCEL_PAYMENT");
        assertThat(message.getTopic()).isEqualTo("payment-commands");
        assertThat(message.getMessageKey()).isEqualTo(booking.getId().toString());

        JsonNode command = body(message);
        assertThat(command.get("commandType").asText()).isEqualTo("CANCEL_PAYMENT");
        assertThat(command.get("payload").get("bookingId").asText()).isEqualTo(booking.getId().toString());
        assertThat(command.get("payload").get("reason").asText()).isEqualTo("Booking cancelled by user");
    }

    @Test
    @DisplayName("a custom topic name from configuration is used")
    void usesConfiguredTopic() {
        BookingKafkaProperties custom = new BookingKafkaProperties("my-commands", null, null, null, null, null, null, null, null, null);
        PaymentCommandOutboxAdapter customAdapter = new PaymentCommandOutboxAdapter(outboxPort, custom, json);

        customAdapter.requestPayment(booking(1));

        assertThat(saved().getTopic()).isEqualTo("my-commands");
    }

    /** The most recent message saved, when several saves happen in one test. */
    private OutboxMessage captureLast() {
        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxPort, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
