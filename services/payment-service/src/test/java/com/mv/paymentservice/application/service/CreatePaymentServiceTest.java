package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.command.CreatePaymentCommand;
import com.mv.paymentservice.application.dto.BookingPaymentContextDto;
import com.mv.paymentservice.application.port.out.BookingPaymentContextPort;
import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.application.port.out.PaymentGatewayPort;
import com.mv.paymentservice.application.port.out.SaveOutboxEventPort;
import com.mv.paymentservice.application.port.out.SavePaymentPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.exception.IdempotencyConflictException;
import com.mv.paymentservice.domain.model.payment.exception.InvalidPaymentException;
import com.mv.paymentservice.domain.model.payment.exception.PaymentExpiredException;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePaymentServiceTest {

    @Mock
    private LoadPaymentPort loadPaymentPort;

    @Mock
    private SavePaymentPort savePaymentPort;

    @Mock
    private BookingPaymentContextPort bookingPaymentContextPort;

    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    @Mock
    private SaveOutboxEventPort saveOutboxEventPort;

    private CreatePaymentService createPaymentService;

    private UUID userId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        createPaymentService = new CreatePaymentService(
                loadPaymentPort,
                savePaymentPort,
                bookingPaymentContextPort,
                paymentGatewayPort,
                saveOutboxEventPort
        );
        userId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create payment successfully when booking is valid and pending")
    void shouldCreatePaymentSuccessfully() {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingId(bookingId)
                .userId(userId)
                .provider("VNPAY")
                .idempotencyKey("idemp-123")
                .requestHash("hash-abc")
                .build();

        BookingPaymentContextDto contextDto = BookingPaymentContextDto.builder()
                .bookingId(bookingId)
                .bookingCode("BK-12345678")
                .userId(userId)
                .amount(new BigDecimal("150000.00"))
                .currency("VND")
                .status("PENDING")
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        when(loadPaymentPort.findByUserIdAndIdempotencyKey(userId, "idemp-123")).thenReturn(Optional.empty());
        when(bookingPaymentContextPort.getPaymentContext(bookingId, userId)).thenReturn(Optional.of(contextDto));
        when(paymentGatewayPort.generatePaymentUrl(any(Payment.class))).thenReturn("https://gateway.example.com/vnpay/pay?paymentId=123");
        when(savePaymentPort.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = createPaymentService.createPayment(command);

        assertThat(payment).isNotNull();
        assertThat(payment.getBookingId()).isEqualTo(bookingId);
        assertThat(payment.getUserId()).isEqualTo(userId);
        assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentUrl()).contains("https://gateway.example.com");

        verify(savePaymentPort).save(any(Payment.class));
        verify(saveOutboxEventPort).save(any(), any(), any());
    }

    @Test
    @DisplayName("Should return existing payment when idempotency key and hash match")
    void shouldReturnExistingPaymentWhenIdempotent() {
        Payment existingPayment = Payment.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .userId(userId)
                .provider("VNPAY")
                .amount(new BigDecimal("150000.00"))
                .currency("VND")
                .status(PaymentStatus.PENDING)
                .idempotencyKey("idemp-123")
                .requestHash("hash-abc")
                .build();

        when(loadPaymentPort.findByUserIdAndIdempotencyKey(userId, "idemp-123"))
                .thenReturn(Optional.of(existingPayment));

        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingId(bookingId)
                .userId(userId)
                .provider("VNPAY")
                .idempotencyKey("idemp-123")
                .requestHash("hash-abc")
                .build();

        Payment payment = createPaymentService.createPayment(command);

        assertThat(payment).isSameAs(existingPayment);
    }

    @Test
    @DisplayName("Should throw IdempotencyConflictException when idempotency key matches but hash differs")
    void shouldThrowConflictWhenHashDiffers() {
        Payment existingPayment = Payment.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .userId(userId)
                .provider("VNPAY")
                .amount(new BigDecimal("150000.00"))
                .currency("VND")
                .status(PaymentStatus.PENDING)
                .idempotencyKey("idemp-123")
                .requestHash("hash-original")
                .build();

        when(loadPaymentPort.findByUserIdAndIdempotencyKey(userId, "idemp-123"))
                .thenReturn(Optional.of(existingPayment));

        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingId(bookingId)
                .userId(userId)
                .provider("VNPAY")
                .idempotencyKey("idemp-123")
                .requestHash("hash-different")
                .build();

        assertThatThrownBy(() -> createPaymentService.createPayment(command))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when booking is not PENDING")
    void shouldThrowExceptionWhenBookingNotPending() {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingId(bookingId)
                .userId(userId)
                .provider("VNPAY")
                .build();

        BookingPaymentContextDto contextDto = BookingPaymentContextDto.builder()
                .bookingId(bookingId)
                .userId(userId)
                .amount(new BigDecimal("150000.00"))
                .status("CONFIRMED")
                .build();

        when(bookingPaymentContextPort.getPaymentContext(bookingId, userId)).thenReturn(Optional.of(contextDto));

        assertThatThrownBy(() -> createPaymentService.createPayment(command))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    @DisplayName("Should throw PaymentExpiredException when booking has expired")
    void shouldThrowExceptionWhenBookingExpired() {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingId(bookingId)
                .userId(userId)
                .provider("VNPAY")
                .build();

        BookingPaymentContextDto contextDto = BookingPaymentContextDto.builder()
                .bookingId(bookingId)
                .userId(userId)
                .amount(new BigDecimal("150000.00"))
                .status("PENDING")
                .expiresAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();

        when(bookingPaymentContextPort.getPaymentContext(bookingId, userId)).thenReturn(Optional.of(contextDto));

        assertThatThrownBy(() -> createPaymentService.createPayment(command))
                .isInstanceOf(PaymentExpiredException.class);
    }
}
