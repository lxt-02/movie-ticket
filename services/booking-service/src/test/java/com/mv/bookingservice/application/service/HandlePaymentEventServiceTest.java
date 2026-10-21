package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.PaymentEventCommand;
import com.mv.bookingservice.application.exception.SeatHoldRejectedException;
import com.mv.bookingservice.application.port.out.InboxPort;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandlePaymentEventServiceTest {

    @Mock
    private InboxPort inboxPort;
    @Mock
    private LoadBookingPort loadBookingPort;
    @Mock
    private SaveBookingPort saveBookingPort;
    @Mock
    private ShowtimeClientPort showtimeClientPort;
    private HandlePaymentEventService service;
    private Booking booking;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        service = new HandlePaymentEventService(inboxPort, loadBookingPort, saveBookingPort, showtimeClientPort,
                new SeatConfirmation(showtimeClientPort));
        booking = Booking.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .holdId(UUID.randomUUID())
                .status(BookingStatus.PENDING)
                .totalAmount(new BigDecimal("150000.00"))
                .currency("VND")
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        paymentId = UUID.randomUUID();
    }

    private PaymentEventCommand event(String type, BigDecimal amount) {
        return PaymentEventCommand.builder()
                .eventId(UUID.randomUUID())
                .eventType(type)
                .bookingId(booking.getId())
                .paymentId(paymentId)
                .amount(amount)
                .currency("VND")
                .build();
    }

    @Test
    void paymentSucceededConfirmsSeatsAndBooking() {
        PaymentEventCommand event = event(PaymentEventCommand.PAYMENT_SUCCEEDED, new BigDecimal("150000"));
        when(inboxPort.isProcessed(event.getEventId())).thenReturn(false);
        when(loadBookingPort.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.handle(event);

        verify(showtimeClientPort).confirmHold(booking.getHoldId());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(paymentId, booking.getPaidPaymentId());
        verify(saveBookingPort).save(booking);
        verify(inboxPort).markProcessed(event.getEventId(), PaymentEventCommand.PAYMENT_SUCCEEDED);
    }

    @Test
    void showtimeFailureKeepsBookingConfirmingForTheRetryJob() {
        PaymentEventCommand event = event(PaymentEventCommand.PAYMENT_SUCCEEDED, new BigDecimal("150000"));
        when(inboxPort.isProcessed(event.getEventId())).thenReturn(false);
        when(loadBookingPort.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(showtimeClientPort.confirmHold(booking.getHoldId())).thenThrow(new RuntimeException("showtime down"));

        service.handle(event);

        assertEquals(BookingStatus.CONFIRMING, booking.getStatus());
        assertEquals(paymentId, booking.getPaidPaymentId());
        verify(saveBookingPort).save(booking);
        verify(inboxPort).markProcessed(event.getEventId(), PaymentEventCommand.PAYMENT_SUCCEEDED);
    }

    @Test
    void seatsRejectedByShowtimeCancelBooking() {
        PaymentEventCommand event = event(PaymentEventCommand.PAYMENT_SUCCEEDED, new BigDecimal("150000"));
        when(inboxPort.isProcessed(event.getEventId())).thenReturn(false);
        when(loadBookingPort.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(showtimeClientPort.confirmHold(booking.getHoldId()))
                .thenThrow(new SeatHoldRejectedException("hold expired"));

        service.handle(event);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(paymentId, booking.getPaidPaymentId());
        verify(saveBookingPort).save(booking);
        verify(inboxPort).markProcessed(event.getEventId(), PaymentEventCommand.PAYMENT_SUCCEEDED);
    }

    @Test
    void paymentForCancelledBookingIsFlaggedForRefund() {
        booking.setStatus(BookingStatus.CANCELLED);
        PaymentEventCommand event = event(PaymentEventCommand.PAYMENT_SUCCEEDED, new BigDecimal("150000"));
        when(inboxPort.isProcessed(event.getEventId())).thenReturn(false);
        when(loadBookingPort.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.handle(event);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertTrue(booking.isNeedsRefund());
        verify(saveBookingPort).save(booking);
        verify(showtimeClientPort, never()).confirmHold(any());
    }

    @Test
    void paymentMadeBeforeDeadlineIsAcceptedEvenIfProcessedAfterIt() {
        booking.setExpiresAt(Instant.now().minusSeconds(30));
        PaymentEventCommand event = event(PaymentEventCommand.PAYMENT_SUCCEEDED, new BigDecimal("150000"));
        event.setPaidAt(Instant.now().minusSeconds(60));
        when(inboxPort.isProcessed(event.getEventId())).thenReturn(false);
        when(loadBookingPort.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.handle(event);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void paymentMadeAfterDeadlineIsNotConfirmed() {
        booking.setExpiresAt(Instant.now().minusSeconds(60));
        PaymentEventCommand event = event(PaymentEventCommand.PAYMENT_SUCCEEDED, new BigDecimal("150000"));
        event.setPaidAt(Instant.now().minusSeconds(10));
        when(inboxPort.isProcessed(event.getEventId())).thenReturn(false);
        when(loadBookingPort.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.handle(event);

        verify(showtimeClientPort, never()).confirmHold(any());
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertTrue(booking.isNeedsRefund());
    }

    @Test
    void providerErrorKeepsBookingPendingSoUserCanPayAgain() {
        PaymentEventCommand event = event(PaymentEventCommand.PAYMENT_CREATE_FAILED, new BigDecimal("150000"));
        event.setReasonCode(PaymentEventCommand.REASON_PROVIDER_ERROR);
        when(inboxPort.isProcessed(event.getEventId())).thenReturn(false);
        when(loadBookingPort.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.handle(event);

        assertEquals(BookingStatus.PENDING, booking.getStatus());
        verify(saveBookingPort, never()).save(any());
        verify(showtimeClientPort, never()).releaseHold(any());
    }

    @Test
    void alreadyProcessedEventIsSkipped() {
        PaymentEventCommand event = event(PaymentEventCommand.PAYMENT_SUCCEEDED, new BigDecimal("150000"));
        when(inboxPort.isProcessed(event.getEventId())).thenReturn(true);

        service.handle(event);

        verify(loadBookingPort, never()).findById(any());
        verify(showtimeClientPort, never()).confirmHold(any());
    }

    @Test
    void amountMismatchDoesNotConfirm() {
        PaymentEventCommand event = event(PaymentEventCommand.PAYMENT_SUCCEEDED, new BigDecimal("1000"));
        when(inboxPort.isProcessed(event.getEventId())).thenReturn(false);
        when(loadBookingPort.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.handle(event);

        verify(showtimeClientPort, never()).confirmHold(any());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertTrue(booking.isNeedsRefund());
        verify(inboxPort).markProcessed(event.getEventId(), PaymentEventCommand.PAYMENT_SUCCEEDED);
    }

    @Test
    void invalidPaymentRequestCancelsBookingAndReleasesHold() {
        PaymentEventCommand event = event(PaymentEventCommand.PAYMENT_CREATE_FAILED, new BigDecimal("150000"));
        event.setReasonCode(PaymentEventCommand.REASON_INVALID_REQUEST);
        when(inboxPort.isProcessed(event.getEventId())).thenReturn(false);
        when(loadBookingPort.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.handle(event);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(saveBookingPort).save(booking);
        verify(showtimeClientPort).releaseHold(booking.getHoldId());
    }

    @Test
    void paymentExpiredExpiresBookingEvenIfReleaseFails() {
        PaymentEventCommand event = event(PaymentEventCommand.PAYMENT_EXPIRED, new BigDecimal("150000"));
        when(inboxPort.isProcessed(event.getEventId())).thenReturn(false);
        when(loadBookingPort.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(showtimeClientPort.releaseHold(booking.getHoldId())).thenThrow(new RuntimeException("showtime down"));

        service.handle(event);

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        verify(saveBookingPort).save(booking);
        verify(inboxPort).markProcessed(event.getEventId(), PaymentEventCommand.PAYMENT_EXPIRED);
    }
}
