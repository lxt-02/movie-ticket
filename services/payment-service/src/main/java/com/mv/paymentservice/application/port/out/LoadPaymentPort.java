package com.mv.paymentservice.application.port.out;

import com.mv.paymentservice.domain.model.payment.aggregate.Payment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadPaymentPort {
    Optional<Payment> findById(UUID id);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /** Latest PENDING/PROCESSING payment of a booking. */
    Optional<Payment> findActiveByBookingId(UUID bookingId);

    Optional<Payment> findLatestByBookingId(UUID bookingId);

    List<Payment> findByBookingId(UUID bookingId);

    /** All payments of a booking, newest first, rows locked (SELECT ... FOR UPDATE). Must run inside a transaction. */
    List<Payment> findByBookingIdForUpdate(UUID bookingId);

    /** Locks the payment row (SELECT ... FOR UPDATE). Must run inside a transaction. */
    Optional<Payment> findByIdForUpdate(UUID id);

    /** Locks the payment row (SELECT ... FOR UPDATE). Must run inside a transaction. */
    Optional<Payment> findByPayosOrderCodeForUpdate(long orderCode);

    /** Overdue PENDING payments, locked with FOR UPDATE SKIP LOCKED. Must run inside a transaction. */
    List<Payment> findDuePendingForUpdate(Instant now, int limit);

    /**
     * PENDING payments created before {@code createdBefore} and not yet overdue, locked with FOR UPDATE SKIP LOCKED.
     * Must run inside a transaction.
     */
    List<Payment> findOpenForReconcileForUpdate(Instant createdBefore, Instant now, int limit);
}
