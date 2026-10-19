package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.dto.BookingPaymentPolicy;
import com.mv.bookingservice.application.port.in.BookingMaintenanceUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.PaymentCommandPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingMaintenanceService implements BookingMaintenanceUseCase {

    static final String EXPIRED_REASON = "Booking expired";

    private final LoadBookingPort loadBookingPort;
    private final SaveBookingPort saveBookingPort;
    private final ShowtimeClientPort showtimeClientPort;
    private final PaymentCommandPort paymentCommandPort;
    private final SeatConfirmation seatConfirmation;
    private final BookingPaymentPolicy policy;

    @Override
    @Transactional
    public int expireOverdueBookings() {
        Instant cutoff = Instant.now().minus(policy.expiryGrace());
        List<Booking> overdue = loadBookingPort.findOverduePendingForUpdate(cutoff, policy.batchSize());
        for (Booking booking : overdue) {
            booking.expire();
            Booking saved = saveBookingPort.save(booking);
            // Close any PayOS link still open so the buyer cannot pay for released seats.
            paymentCommandPort.cancelPayments(saved, EXPIRED_REASON);
            releaseHoldQuietly(saved);
            log.info("Booking {} EXPIRED (expires_at {})", saved.getId(), saved.getExpiresAt());
        }
        return overdue.size();
    }

    @Override
    @Transactional
    public int retrySeatConfirmations() {
        int confirmed = 0;
        for (Booking booking : loadBookingPort.findConfirmingForUpdate(policy.batchSize())) {
            SeatConfirmation.Outcome outcome = seatConfirmation.tryConfirm(booking);
            if (outcome.changed()) {
                // CONFIRMED, or REFUND_PENDING: either way it leaves the CONFIRMING retry queue.
                saveBookingPort.save(booking);
            }
            if (outcome == SeatConfirmation.Outcome.CONFIRMED) {
                confirmed++;
            }
        }
        return confirmed;
    }

    private void releaseHoldQuietly(Booking booking) {
        if (booking.getHoldId() == null) {
            return;
        }
        try {
            showtimeClientPort.releaseHold(booking.getHoldId());
        } catch (RuntimeException e) {
            // Showtime expires the hold on its own.
            log.warn("Could not release hold {} of booking {}: {}", booking.getHoldId(), booking.getId(), e.getMessage());
        }
    }
}
