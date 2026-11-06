-- Outbox: outbox_events -> outbox_messages (same shape as payment-service / movie-service).
-- Each row now carries its own topic and Kafka key, so the relay no longer maps event_type -> topic.
--   PENDING    not sent yet (or a temporary error) -> retried with back-off
--   PROCESSING reserved by the shared model; the booking relay publishes under outbox_relay_lock and does not use it
--   PUBLISHED  broker acknowledged it (published_at set)
--   FAILED     permanent error: later commands of the SAME booking wait behind it; requeue after the fix
-- Inbox removed: payment-events are applied idempotently by booking state, inbox_events is dropped.
CREATE TABLE outbox_messages (
    id             UUID          PRIMARY KEY,
    aggregate_type VARCHAR(100)  NOT NULL,
    aggregate_id   UUID          NOT NULL,
    event_type     VARCHAR(150)  NOT NULL,
    topic          VARCHAR(255)  NOT NULL,
    message_key    VARCHAR(255)  NOT NULL,
    payload        JSONB         NOT NULL,
    status         VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    retry_count    INT           NOT NULL DEFAULT 0,
    next_retry_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at   TIMESTAMPTZ,
    last_error     VARCHAR(2000),
    CONSTRAINT ck_booking_outbox_messages_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_booking_outbox_messages_retry_count CHECK (retry_count >= 0)
);

-- Existing rows: every booking command went to payment-commands with key = bookingId.
INSERT INTO outbox_messages (id, aggregate_type, aggregate_id, event_type, topic, message_key, payload,
                             status, retry_count, next_retry_at, created_at, published_at, last_error)
SELECT id, 'BOOKING', aggregate_id, event_type, 'payment-commands', CAST(aggregate_id AS VARCHAR(255)), payload,
       status, retry_count, next_retry_at, created_at, published_at, last_error
FROM outbox_events;

CREATE INDEX idx_booking_outbox_messages_status_created ON outbox_messages (status, created_at);
CREATE INDEX idx_booking_outbox_messages_aggregate ON outbox_messages (aggregate_type, aggregate_id, status);

DROP TABLE outbox_events;
DROP TABLE IF EXISTS inbox_events;
