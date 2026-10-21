package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.dto.BookingPaymentPolicy;
import com.mv.bookingservice.application.exception.SeatHoldRejectedException;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * CONFIRMING -> CONFIRMED once Showtime has turned the held seats into BOOKED.
 * <ul>
 *     <li>Showtime confirms: CONFIRMED.</li>
 *     <li>Showtime refuses for good (409/404, or a non-CONFIRMED hold): the seats are gone, so the paid booking
 *     is CANCELLED right away instead of being retried forever. No refund flow: logged for manual handling.</li>
 *     <li>Temporary failure (timeout, 5xx): stays CONFIRMING for the maintenance job, but only until
 *     {@link BookingPaymentPolicy#confirmationTimeout()} after the payment; then CANCELLED too.</li>
 * </ul>
 * Never saves: the caller saves when {@link Outcome#changed()} is true.
 */
@Slf4j
@Component
public class SeatConfirmation {

    public enum Outcome {
        /** Seats BOOKED, booking CONFIRMED. */
        CONFIRMED,
        /** Seats cannot be booked, booking CANCELLED. */
        FAILED,
        /** Temporary failure, booking still CONFIRMING. */
        RETRY_LATER,
        /** Booking was not CONFIRMING: nothing done. */
        SKIPPED;

        public boolean changed() {
            return this == CONFIRMED || this == FAILED;
        }
    }

    static final Duration DEFAULT_CONFIRMATION_TIMEOUT = Duration.ofMinutes(30);

    private final ShowtimeClientPort showtimeClientPort;
    private final Duration confirmationTimeout;

    @Autowired
    public SeatConfirmation(ShowtimeClientPort showtimeClientPort, BookingPaymentPolicy policy) {
        this(showtimeClientPort, policy.confirmationTimeout());
    }

    public SeatConfirmation(ShowtimeClientPort showtimeClientPort) {
        this(showtimeClientPort, DEFAULT_CONFIRMATION_TIMEOUT);
    }

    public SeatConfirmation(ShowtimeClientPort showtimeClientPort, Duration confirmationTimeout) {
        this.showtimeClientPort = showtimeClientPort;
        this.confirmationTimeout = confirmationTimeout;
    }

    public Outcome tryConfirm(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMING) {
            return Outcome.SKIPPED;
        }
        try {
            showtimeClientPort.confirmHold(booking.getHoldId());
        } catch (SeatHoldRejectedException e) {
            return fail(booking, "Showtime rejected the seats: " + e.getMessage());
        } catch (RuntimeException e) {
            if (confirmingTooLong(booking)) {
                return fail(booking, "Seats still not confirmed " + confirmationTimeout
                        + " after payment (last error: " + e.getMessage() + ")");
            }
            log.warn("Seat confirmation failed for booking {} (hold {}), stays CONFIRMING: {}",
                    booking.getId(), booking.getHoldId(), e.getMessage());
            return Outcome.RETRY_LATER;
        }
        booking.confirm(booking.getPaidPaymentId());
        log.info("Booking {} CONFIRMED by payment {}", booking.getId(), booking.getPaidPaymentId());
        return Outcome.CONFIRMED;
    }

    /** updatedAt is set by markConfirming and not touched by failed attempts (they do not save). */
    private boolean confirmingTooLong(Booking booking) {
        Instant since = booking.getUpdatedAt();
        return since != null && Instant.now().isAfter(since.plus(confirmationTimeout));
    }

    private Outcome fail(Booking booking, String reason) {
        booking.failAfterPayment();
        try {
            // After a timeout the hold may still be alive: give the seats back. Best effort only.
            showtimeClientPort.releaseHold(booking.getHoldId());
        } catch (RuntimeException e) {
            log.warn("Could not release hold {} of booking {}: {}", booking.getHoldId(), booking.getId(), e.getMessage());
        }
        log.error("MANUAL CHECK: booking {} paid by {} but CANCELLED, seats not booked. {}",
                booking.getId(), booking.getPaidPaymentId(), reason);
        booking.flagForRefund("Paid by " + booking.getPaidPaymentId() + " but seats could not be booked: " + reason);
        return Outcome.FAILED;
    }
}
