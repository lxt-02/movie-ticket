package com.mv.paymentservice.infrastructure.messaging;

/**
 * A consumed record that can never be processed as it is (malformed JSON, missing fields). Retrying would fail the
 * same way, so the error handler sends it straight to the dead-letter topic (kept with the error in its headers,
 * visible in Kafka UI) instead of dropping it or blocking the partition.
 */
public class NonRetryableMessageException extends RuntimeException {

    public NonRetryableMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
