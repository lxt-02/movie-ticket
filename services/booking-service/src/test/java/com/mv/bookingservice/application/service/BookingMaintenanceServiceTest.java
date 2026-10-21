package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.dto.BookingPaymentPolicy;
import com.mv.bookingservice.application.exception.SeatHoldRejectedException;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.PaymentCommandPort;
import com.mv.bookingservice.application.port.out.PaymentStatusPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingMaintenanceServiceTest {

    @Mock
    private LoadBookingPort loadBookingPort;
    @Mock
    private SaveBookingPort saveBookingPort;
    @Mock
    private ShowtimeClientPort showtimeClientPort;
    @Mock
    private PaymentCommandPort paymentCommandPort;
    @Mock
    private PaymentStatusPort paymentStatusPort;

    private BookingMaintenanceService service;

    @BeforeEach
    void setUp() {
        service = new BookingMaintenanceService(loadBookingPort, saveBookingPort, showtimeClientPort, paymentCommandPort,
                new SeatConfirmation(showtimeClientPort), new BookingPaymentPolicy(3, Duration.ofSeconds(90), 50),
                paymentStatusPort);
    }

    private Booking booking(BookingStatus status, Instant expiresAt) {
        return Booking.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .holdId(UUID.randomUUID())
                .status(status)
                .totalAmount(new BigDecimal("150000.00"))
                .currency("VND")
                .expiresAt(expiresAt)
                .paidPaymentId(status == BookingStatus.CONFIRMING ? UUID.randomUUID() : null)
                .build();
    }

    @Test
    void expiresOverdueBookingsAfterGraceAndCancelsTheirPayments() {
        Booking overdue = booking(BookingStatus.PENDING, Instant.now().minusSeconds(300));
        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        when(loadBookingPort.findOverduePendingForUpdate(cutoff.capture(), eq(50))).thenReturn(List.of(overdue));
        when(saveBookingPort.save(overdue)).thenReturn(overdue);

        int expired = service.expireOverdueBookings();

        assertEquals(1, expired);
        assertEquals(BookingStatus.EXPIRED, overdue.getStatus());
        assertTrue(cutoff.getValue().isBefore(Instant.now().minusSeconds(89)));
        verify(paymentCommandPort).cancelPayments(overdue, BookingMaintenanceService.EXPIRED_REASON);
        verify(showtimeClientPort).releaseHold(overdue.getHoldId());
    }

    @Test
    void retriesSeatConfirmationOfConfirmingBookings() {
        Booking confirming = booking(BookingStatus.CONFIRMING, Instant.now().minusSeconds(60));
        when(loadBookingPort.findConfirmingForUpdate(50)).thenReturn(List.of(confirming));

        int confirmed = service.retrySeatConfirmations();

        assertEquals(1, confirmed);
        assertEquals(BookingStatus.CONFIRMED, confirming.getStatus());
        verify(saveBookingPort).save(confirming);
    }

    @Test
    void confirmingBookingStaysWhenShowtimeStillFails() {
        Booking confirming = booking(BookingStatus.CONFIRMING, Instant.now().minusSeconds(60));
        when(loadBookingPort.findConfirmingForUpdate(50)).thenReturn(List.of(confirming));
        when(showtimeClientPort.confirmHold(confirming.getHoldId())).thenThrow(new RuntimeException("down"));

        assertEquals(0, service.retrySeatConfirmations());
        assertEquals(BookingStatus.CONFIRMING, confirming.getStatus());
        verify(saveBookingPort, never()).save(any());
    }

    @Test
    void rejectedSeatsCancelBookingInsteadOfRetryingForever() {
        Booking confirming = booking(BookingStatus.CONFIRMING, Instant.now().minusSeconds(60));
        when(loadBookingPort.findConfirmingForUpdate(50)).thenReturn(List.of(confirming));
        when(showtimeClientPort.confirmHold(confirming.getHoldId()))
                .thenThrow(new SeatHoldRejectedException("hold expired"));

        assertEquals(0, service.retrySeatConfirmations());
        assertEquals(BookingStatus.CANCELLED, confirming.getStatus());
        verify(saveBookingPort).save(confirming);
        verify(showtimeClientPort).releaseHold(confirming.getHoldId());
    }

    @Test
    void showtimeDownForTooLongCancelsBooking() {
        Booking confirming = booking(BookingStatus.CONFIRMING, Instant.now().minusSeconds(3600));
        confirming.setUpdatedAt(Instant.now().minus(Duration.ofMinutes(31)));
        when(loadBookingPort.findConfirmingForUpdate(50)).thenReturn(List.of(confirming));
        when(showtimeClientPort.confirmHold(confirming.getHoldId())).thenThrow(new RuntimeException("down"));

        assertEquals(0, service.retrySeatConfirmations());
        assertEquals(BookingStatus.CANCELLED, confirming.getStatus());
        verify(saveBookingPort).save(confirming);
    }

    @Test
    void paidBookingWhoseEventWasLostIsConfirmedInsteadOfExpired() {
        Booking overdue = booking(BookingStatus.PENDING, Instant.now().minusSeconds(120));
        UUID paymentId = UUID.randomUUID();
        when(loadBookingPort.findOverduePendingForUpdate(any(), eq(50))).thenReturn(List.of(overdue));
        when(paymentStatusPort.findSucceededPayment(overdue.getId())).thenReturn(Optional.of(
                new PaymentStatusPort.PaidPayment(paymentId, new BigDecimal("150000"), "VND",
                        Instant.now().minusSeconds(200))));

        int expired = service.expireOverdueBookings();

        assertEquals(0, expired);
        assertEquals(BookingStatus.CONFIRMED, overdue.getStatus());
        assertEquals(paymentId, overdue.getPaidPaymentId());
        verify(showtimeClientPort).confirmHold(overdue.getHoldId());
        verify(saveBookingPort).save(overdue);
        verify(paymentCommandPort, never()).cancelPayments(any(), any());
    }

    @Test
    void expiryIsPostponedWhenPaymentServiceIsDownAndSeatsAreStillHeld() {
        Booking overdue = booking(BookingStatus.PENDING, Instant.now().minusSeconds(100));
        when(loadBookingPort.findOverduePendingForUpdate(any(), eq(50))).thenReturn(List.of(overdue));
        when(paymentStatusPort.findSucceededPayment(overdue.getId())).thenThrow(new RuntimeException("down"));

        assertEquals(0, service.expireOverdueBookings());
        assertEquals(BookingStatus.PENDING, overdue.getStatus());
        verify(saveBookingPort, never()).save(any());
    }

    @Test
    void bookingExpiresWhenPaymentServiceIsDownAndSeatHoldIsOver() {
        Booking overdue = booking(BookingStatus.PENDING, Instant.now().minusSeconds(600));
        when(loadBookingPort.findOverduePendingForUpdate(any(), eq(50))).thenReturn(List.of(overdue));
        when(paymentStatusPort.findSucceededPayment(overdue.getId())).thenThrow(new RuntimeException("down"));
        when(saveBookingPort.save(overdue)).thenReturn(overdue);

        assertEquals(1, service.expireOverdueBookings());
        assertEquals(BookingStatus.EXPIRED, overdue.getStatus());
    }

    @Test
    void paymentAfterDeadlineIsFlaggedForRefundAndBookingExpires() {
        Booking overdue = booking(BookingStatus.PENDING, Instant.now().minusSeconds(120));
        when(loadBookingPort.findOverduePendingForUpdate(any(), eq(50))).thenReturn(List.of(overdue));
        when(paymentStatusPort.findSucceededPayment(overdue.getId())).thenReturn(Optional.of(
                new PaymentStatusPort.PaidPayment(UUID.randomUUID(), new BigDecimal("150000"), "VND",
                        Instant.now().minusSeconds(10))));
        when(saveBookingPort.save(overdue)).thenReturn(overdue);

        assertEquals(1, service.expireOverdueBookings());
        assertEquals(BookingStatus.EXPIRED, overdue.getStatus());
        assertTrue(overdue.isNeedsRefund());
    }
}
