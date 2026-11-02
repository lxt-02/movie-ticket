package com.mv.bookingservice.application.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Runs a side effect on another service only once the booking change is committed.
 * <p>
 * Example: releasing the seat hold in Showtime while expiring a booking. If the release ran before the commit and
 * the commit then failed (database error, lock conflict), the booking would still be PENDING with its seats already
 * given away; a payment arriving next would then be taken for seats that cannot be confirmed any more.
 * Outside a transaction (unit tests, plain calls) the action runs immediately.
 */
final class AfterCommit {

    private AfterCommit() {
    }

    static void run(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
