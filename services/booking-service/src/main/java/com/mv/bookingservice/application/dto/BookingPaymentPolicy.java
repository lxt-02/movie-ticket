package com.mv.bookingservice.application.dto;

import java.time.Duration;

/**
 * Tunables for payment retries and booking expiry (booking.payment.* / booking.expiry.*).
 *
 * @param maxPaymentAttempts payment links allowed per booking (first one included)
 * @param expiryGrace        a PENDING booking is expired only this long after expires_at, so a transfer made just
 *                           before the deadline can still arrive
 * @param batchSize          bookings handled per job run
 * @param confirmationTimeout a paid booking whose seats Showtime still has not confirmed this long after payment
 *                            (Showtime down) stops being retried and moves to REFUND_PENDING
 * @param holdSafetyMargin   the payment deadline (booking expires_at = PayOS link expiredAt) is set this much
 *                           before the Showtime seat hold ends. A payment made just before the deadline, whose
 *                           webhook arrives late, still finds the seats held. Must exceed {@code expiryGrace}, so
 *                           the expiry job also runs while the hold is still alive.
 */
public record BookingPaymentPolicy(int maxPaymentAttempts, Duration expiryGrace, int batchSize,
                                   Duration confirmationTimeout, Duration holdSafetyMargin) {

    public static final Duration DEFAULT_CONFIRMATION_TIMEOUT = Duration.ofMinutes(30);
    public static final Duration DEFAULT_HOLD_SAFETY_MARGIN = Duration.ofMinutes(3);

    public BookingPaymentPolicy {
        if (holdSafetyMargin == null || expiryGrace == null || holdSafetyMargin.compareTo(expiryGrace) <= 0) {
            throw new IllegalArgumentException("holdSafetyMargin (" + holdSafetyMargin
                    + ") must be longer than expiryGrace (" + expiryGrace + ")");
        }
    }

    public BookingPaymentPolicy(int maxPaymentAttempts, Duration expiryGrace, int batchSize) {
        this(maxPaymentAttempts, expiryGrace, batchSize, DEFAULT_CONFIRMATION_TIMEOUT, DEFAULT_HOLD_SAFETY_MARGIN);
    }
}
