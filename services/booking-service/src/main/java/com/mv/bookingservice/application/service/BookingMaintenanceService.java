package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.dto.BookingPaymentPolicy;
import com.mv.bookingservice.application.port.in.BookingMaintenanceUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.PaymentCommandPort;
import com.mv.bookingservice.application.port.out.PaymentStatusPort;
import com.mv.bookingservice.application.port.out.PaymentStatusPort.PaidPayment;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Background jobs of booking-service.
 * <ul>
 *     <li>Expiry: PENDING bookings past their deadline (+ grace) are expired and their seats released. Before that,
 *     payment-service is asked directly whether the booking is in fact paid: a PAYMENT_SUCCEEDED event that was
 *     lost or is still parked in the dead-letter topic must not cost a buyer the seats they paid for.</li>
 *     <li>Seat confirmation retry for CONFIRMING bookings (Showtime was down when the payment arrived).</li>
 * </ul>
 */
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
    private final PaymentStatusPort paymentStatusPort;

    @Override
    @Transactional
    public int expireOverdueBookings() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(policy.expiryGrace());
        List<Booking> overdue = loadBookingPort.findOverduePendingForUpdate(cutoff, policy.batchSize());
        int expired = 0;
        for (Booking booking : overdue) {
            Optional<PaidPayment> paid;
            try {
                paid = paymentStatusPort.findSucceededPayment(booking.getId());
            } catch (RuntimeException e) {
                if (holdStillAlive(booking, now)) {
                    // Do not guess: the seats are still held, so wait for the next run.
                    log.warn("Cannot ask payment-service about booking {} ({}); expiry postponed to the next run",
                            booking.getId(), e.getMessage());
                    continue;
                }
                // The seat hold is over anyway: expire. A payment found later is flagged for refund.
                log.warn("payment-service unreachable for booking {} and its seat hold is over: expiring it ({})",
                        booking.getId(), e.getMessage());
                paid = Optional.empty();
            }
            if (paid.isPresent() && recoverPaidBooking(booking, paid.get())) {
                continue;
            }
            booking.expire();
            Booking saved = saveBookingPort.save(booking);
            // Close any PayOS link still open so the buyer cannot pay for released seats.
            paymentCommandPort.cancelPayments(saved, EXPIRED_REASON);
            releaseHoldQuietly(saved);
            expired++;
            log.info("Booking {} EXPIRED (expires_at {})", saved.getId(), saved.getExpiresAt());
        }
        return expired;
    }

    /** The Showtime seat hold ends {@code holdSafetyMargin} after the payment deadline. */
    private boolean holdStillAlive(Booking booking, Instant now) {
        return booking.getExpiresAt() != null && now.isBefore(booking.getExpiresAt().plus(policy.holdSafetyMargin()));
    }

    /**
     * The booking is paid but its PAYMENT_SUCCEEDED event was never applied: confirm it instead of expiring it.
     *
     * @return false if the payment cannot be used for this booking (wrong amount, paid after the deadline): the
     * booking is flagged for refund and expires as usual
     */
    private boolean recoverPaidBooking(Booking booking, PaidPayment payment) {
        boolean sameAmount = payment.amount() != null && booking.getTotalAmount() != null
                && payment.amount().compareTo(booking.getTotalAmount()) == 0
                && payment.currency() != null && payment.currency().equalsIgnoreCase(booking.getCurrency());
        if (!sameAmount || !booking.wasPaidInTime(payment.paidAt())) {
            String reason = "Payment " + payment.paymentId() + " (" + payment.amount() + " " + payment.currency()
                    + ", paid at " + payment.paidAt() + ") cannot be used: booking total " + booking.getTotalAmount()
                    + " " + booking.getCurrency() + ", deadline " + booking.getExpiresAt();
            log.error("MANUAL CHECK: booking {}: {}", booking.getId(), reason);
            booking.flagForRefund(reason);
            return false;
        }
        log.warn("SAFETY NET: booking {} is PAID by {} but PAYMENT_SUCCEEDED was never applied; confirming it "
                + "instead of expiring it", booking.getId(), payment.paymentId());
        booking.markConfirming(payment.paymentId(), payment.paidAt());
        // CONFIRMED, CANCELLED (seats lost, flagged) or still CONFIRMING (Showtime down: the retry job goes on).
        seatConfirmation.tryConfirm(booking);
        saveBookingPort.save(booking);
        return true;
    }

    @Override
    @Transactional
    public int retrySeatConfirmations() {
        int confirmed = 0;
        for (Booking booking : loadBookingPort.findConfirmingForUpdate(policy.batchSize())) {
            SeatConfirmation.Outcome outcome = seatConfirmation.tryConfirm(booking);
            if (outcome.changed()) {
                // CONFIRMED, or CANCELLED (seats lost): either way it leaves the CONFIRMING retry queue.
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
