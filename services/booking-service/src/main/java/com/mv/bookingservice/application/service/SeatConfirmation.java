package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.dto.BookingPaymentPolicy;
import com.mv.bookingservice.application.exception.SeatHoldRejectedException;
import com.mv.bookingservice.application.port.out.PaymentCommandPort;
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
 *     moves to REFUND_PENDING right away instead of being retried forever, and REFUND_PAYMENT goes to the outbox.</li>
 *     <li>Temporary failure (timeout, 5xx): stays CONFIRMING for the maintenance job, but only until
 *     {@link BookingPaymentPolicy#confirmationTimeout()} after the payment; then REFUND_PENDING too.</li>
 * </ul>
 * Never saves: the caller saves when {@link Outcome#changed()} is true.
 */
@Slf4j
@Component
public class SeatConfirmation {

    public enum Outcome {
        /** Seats BOOKED, booking CONFIRMED. */
        CONFIRMED,
        /** Seats cannot be booked, booking REFUND_PENDING. */
        REFUND_REQUIRED,
        /** Temporary failure, booking still CONFIRMING. */
        RETRY_LATER,
        /** Booking was not CONFIRMING: nothing done. */
        SKIPPED;

        public boolean changed() {
            return this == CONFIRMED || this == REFUND_REQUIRED;
        }
    }

    static final Duration DEFAULT_CONFIRMATION_TIMEOUT = Duration.ofMinutes(30);

    private final ShowtimeClientPort showtimeClientPort;
    private final PaymentCommandPort paymentCommandPort;
    private final Duration confirmationTimeout;

    @Autowired
    public SeatConfirmation(ShowtimeClientPort showtimeClientPort, PaymentCommandPort paymentCommandPort,
                            BookingPaymentPolicy policy) {
        this(showtimeClientPort, paymentCommandPort, policy.confirmationTimeout());
    }

    public SeatConfirmation(ShowtimeClientPort showtimeClientPort, PaymentCommandPort paymentCommandPort) {
        this(showtimeClientPort, paymentCommandPort, DEFAULT_CONFIRMATION_TIMEOUT);
    }

    public SeatConfirmation(ShowtimeClientPort showtimeClientPort, PaymentCommandPort paymentCommandPort,
                            Duration confirmationTimeout) {
        this.showtimeClientPort = showtimeClientPort;
        this.paymentCommandPort = paymentCommandPort;
        this.confirmationTimeout = confirmationTimeout;
    }

    public Outcome tryConfirm(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMING) {
            return Outcome.SKIPPED;
        }
        try {
            showtimeClientPort.confirmHold(booking.getHoldId());
        } catch (SeatHoldRejectedException e) {
            return refund(booking, "Showtime rejected the seats: " + e.getMessage());
        } catch (RuntimeException e) {
            if (confirmingTooLong(booking)) {
                return refund(booking, "Seats still not confirmed " + confirmationTimeout
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

    private Outcome refund(Booking booking, String reason) {
        booking.requestRefund();
        paymentCommandPort.requestRefund(booking, booking.getPaidPaymentId(), reason);
        log.error("REFUND NEEDED: booking {} paid by {} moved to REFUND_PENDING. {}",
                booking.getId(), booking.getPaidPaymentId(), reason);
        return Outcome.REFUND_REQUIRED;
    }
}
