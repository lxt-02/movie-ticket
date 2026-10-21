package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.PaymentEventCommand;
import com.mv.bookingservice.application.port.in.HandlePaymentEventUseCase;
import com.mv.bookingservice.application.port.out.InboxPort;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Applies payment-events to bookings. The inbox row and the booking change commit together, so a redelivered
 * event is skipped. Business mismatches are logged and acknowledged (retrying cannot fix them); only technical
 * errors (database, Showtime call) are thrown so Kafka redelivers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandlePaymentEventService implements HandlePaymentEventUseCase {

    private final InboxPort inboxPort;
    private final LoadBookingPort loadBookingPort;
    private final SaveBookingPort saveBookingPort;
    private final ShowtimeClientPort showtimeClientPort;
    private final SeatConfirmation seatConfirmation;

    @Override
    @Transactional
    public void handle(PaymentEventCommand event) {
        if (event.getEventId() == null || event.getBookingId() == null) {
            log.warn("Ignoring payment event without eventId/bookingId: {}", event.getEventType());
            return;
        }
        if (inboxPort.isProcessed(event.getEventId())) {
            log.debug("Payment event {} already processed", event.getEventId());
            return;
        }

        Optional<Booking> found = loadBookingPort.findById(event.getBookingId());
        if (found.isEmpty()) {
            log.warn("Payment event {} for unknown booking {}", event.getEventType(), event.getBookingId());
        } else {
            apply(found.get(), event);
        }
        inboxPort.markProcessed(event.getEventId(), event.getEventType());
    }

    private void apply(Booking booking, PaymentEventCommand event) {
        String type = event.getEventType() == null ? "" : event.getEventType();
        switch (type) {
            case PaymentEventCommand.PAYMENT_SUCCEEDED -> onPaymentSucceeded(booking, event);
            case PaymentEventCommand.PAYMENT_CREATE_FAILED -> onPaymentCreateFailed(booking, event);
            case PaymentEventCommand.PAYMENT_EXPIRED -> onPaymentExpired(booking);
            case PaymentEventCommand.PAYMENT_CANCELLED ->
                    // An earlier link replaced by "pay again", or closed because the booking ended.
                    log.info("Payment {} of booking {} cancelled: {}", event.getPaymentId(), booking.getId(), event.getReason());
            case PaymentEventCommand.PAYMENT_LINK_CREATED ->
                    log.info("Payment link ready for booking {} (payment {})", booking.getId(), event.getPaymentId());
            case PaymentEventCommand.PAYMENT_FAILED ->
                    // One failed attempt does not cancel the booking; it still expires with its seat hold.
                    log.warn("Payment {} failed for booking {}: {}", event.getPaymentId(), booking.getId(), event.getReason());
            default -> log.warn("Unknown payment event type {} for booking {}", type, booking.getId());
        }
    }

    private void onPaymentSucceeded(Booking booking, PaymentEventCommand event) {
        if (event.getAmount() == null || event.getAmount().compareTo(booking.getTotalAmount()) != 0
                || event.getCurrency() == null || !event.getCurrency().equalsIgnoreCase(booking.getCurrency())) {
            // Booking stays as it is (still PENDING: the buyer can pay again); the payment is checked manually.
            manualCheck(booking, event.getPaymentId(), "Paid " + event.getAmount() + " " + event.getCurrency()
                    + " but booking total is " + booking.getTotalAmount() + " " + booking.getCurrency());
            saveBookingPort.save(booking);
            return;
        }

        BookingStatus status = booking.getStatus();
        if (status == BookingStatus.CONFIRMED) {
            if (event.getPaymentId() != null && !event.getPaymentId().equals(booking.getPaidPaymentId())) {
                manualCheck(booking, event.getPaymentId(), "Booking already paid by " + booking.getPaidPaymentId());
                saveBookingPort.save(booking);
            }
            return;
        }
        if (status == BookingStatus.CONFIRMING) {
            if (event.getPaymentId() != null && !event.getPaymentId().equals(booking.getPaidPaymentId())) {
                manualCheck(booking, event.getPaymentId(), "Booking already paid by " + booking.getPaidPaymentId());
                saveBookingPort.save(booking);
            } else if (seatConfirmation.tryConfirm(booking).changed()) {
                saveBookingPort.save(booking);
            }
            return;
        }
        if (status != BookingStatus.PENDING) {
            // CANCELLED / EXPIRED: seats already released.
            manualCheck(booking, event.getPaymentId(), "Payment succeeded but booking is " + status);
            saveBookingPort.save(booking);
            return;
        }

        // Judge by when the buyer paid, not by when the event arrives (webhook / reconcile can be late).
        Instant paidAt = event.getPaidAt() != null ? event.getPaidAt() : Instant.now();
        if (!booking.wasPaidInTime(paidAt)) {
            // Too late: the seat hold is gone. The booking expires like an unpaid one.
            booking.expire();
            manualCheck(booking, event.getPaymentId(),
                    "Paid at " + paidAt + " after the payment deadline " + booking.getExpiresAt());
            saveBookingPort.save(booking);
            releaseHoldQuietly(booking);
            return;
        }

        booking.markConfirming(event.getPaymentId(), paidAt);
        // Temporary Showtime failure keeps the booking CONFIRMING (saved below) for the maintenance job;
        // a permanent refusal cancels it.
        seatConfirmation.tryConfirm(booking);
        saveBookingPort.save(booking);
    }

    private void onPaymentCreateFailed(Booking booking, PaymentEventCommand event) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }
        if (PaymentEventCommand.REASON_PROVIDER_ERROR.equals(event.getReasonCode())) {
            // PayOS trouble is temporary: keep the seats, the buyer can "pay again" until the booking expires.
            log.warn("Payment link for booking {} could not be created ({}); booking stays PENDING",
                    booking.getId(), event.getReason());
            return;
        }
        booking.cancel();
        saveBookingPort.save(booking);
        releaseHoldQuietly(booking);
        log.warn("Booking {} CANCELLED: payment request rejected ({})", booking.getId(), event.getReason());
    }

    private void onPaymentExpired(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }
        booking.expire();
        saveBookingPort.save(booking);
        releaseHoldQuietly(booking);
        log.info("Booking {} EXPIRED with its payment link", booking.getId());
    }

    /**
     * Money that the booking cannot use. No refund flow in this system: the booking is flagged
     * ({@code needs_refund}, listed by GET /api/v1/bookings/admin/needs-refund) and logged. The caller saves.
     */
    private void manualCheck(Booking booking, java.util.UUID paymentId, String reason) {
        log.error("MANUAL CHECK: booking {} payment {}: {}", booking.getId(), paymentId, reason);
        booking.flagForRefund("Payment " + paymentId + ": " + reason);
    }

    private void releaseHoldQuietly(Booking booking) {
        if (booking.getHoldId() == null) {
            return;
        }
        try {
            showtimeClientPort.releaseHold(booking.getHoldId());
        } catch (RuntimeException e) {
            // Showtime expires the hold on its own; the booking state change must not be lost.
            log.warn("Could not release hold {} of booking {}: {}", booking.getHoldId(), booking.getId(), e.getMessage());
        }
    }
}
