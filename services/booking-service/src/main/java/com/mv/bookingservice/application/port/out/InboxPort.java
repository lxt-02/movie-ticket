package com.mv.bookingservice.application.port.out;

import java.util.UUID;

/**
 * Remembers consumed Kafka events (inbox_events) so a redelivered event is applied only once.
 */
public interface InboxPort {
    boolean isProcessed(UUID eventId);

    void markProcessed(UUID eventId, String eventType);
}
