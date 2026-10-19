package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.RetryBookingPaymentCommand;
import com.mv.bookingservice.application.dto.BookingPaymentPolicy;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.PaymentCommandPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import com.mv.bookingservice.domain.model.booking.exception.InvalidBookingStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryBookingPaymentServiceTest {

    @Mock
    private LoadBookingPort loadBookingPort;
    @Mock
    private SaveBookingPort saveBookingPort;
    @Mock
    private PaymentCommandPort paymentCommandPort;

    private RetryBookingPaymentService service;
    private Booking booking;

    @BeforeEach
    void setUp() {
        service = new RetryBookingPaymentService(loadBookingPort, saveBookingPort, paymentCommandPort,
                new BookingPaymentPolicy(3, Duration.ofSeconds(90), 50));
        booking = Booking.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .status(BookingStatus.PENDING)
                .totalAmount(new BigDecimal("150000.00"))
                .currency("VND")
                .expiresAt(Instant.now().plusSeconds(300))
                .paymentAttempt(1)
                .build();
        when(loadBookingPort.findById(booking.getId())).thenReturn(Optional.of(booking));
    }

    private RetryBookingPaymentCommand command(UUID userId) {
        return RetryBookingPaymentCommand.builder().bookingId(booking.getId()).userId(userId).build();
    }

    @Test
    void startsNextAttemptAndSendsCreatePayment() {
        when(saveBookingPort.save(booking)).thenReturn(booking);

        Booking result = service.execute(command(booking.getUserId()));

        assertEquals(2, result.currentPaymentAttempt());
        verify(paymentCommandPort).requestPayment(booking);
    }

    @Test
    void refusesAfterMaxAttempts() {
        booking.setPaymentAttempt(3);

        assertThrows(BookingValidationException.class, () -> service.execute(command(booking.getUserId())));
        verify(paymentCommandPort, never()).requestPayment(any());
    }

    @Test
    void refusesExpiredOrPaidBooking() {
        booking.setExpiresAt(Instant.now().minusSeconds(1));
        assertThrows(InvalidBookingStateException.class, () -> service.execute(command(booking.getUserId())));

        booking.setExpiresAt(Instant.now().plusSeconds(300));
        booking.setStatus(BookingStatus.CONFIRMED);
        assertThrows(InvalidBookingStateException.class, () -> service.execute(command(booking.getUserId())));
        verify(paymentCommandPort, never()).requestPayment(any());
    }

    @Test
    void refusesMissingUser() {
        assertThrows(BookingValidationException.class, () -> service.execute(command(null)));
        verify(saveBookingPort, never()).save(any());
    }

    @Test
    void refusesOtherUser() {
        assertThrows(BookingValidationException.class, () -> service.execute(command(UUID.randomUUID())));
        verify(saveBookingPort, never()).save(any());
    }
}
