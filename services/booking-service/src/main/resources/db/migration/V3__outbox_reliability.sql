-- Outbox reliability: an outbox row now records its delivery state instead of only published_at.
--   PENDING   not sent yet (or a temporary error: broker down, topic not found, timeout) -> retried with back-off
--   PUBLISHED broker acknowledged it (published_at set)
--   FAILED    permanent error (no permission on the topic, record too large, unknown event type...):
--             not retried automatically; later commands of the SAME booking wait behind it (order kept),
--             other bookings keep flowing. Fix the cause, then POST /api/v1/bookings/admin/outbox/requeue-failed.
ALTER TABLE outbox_events
    ADD COLUMN status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    ADD COLUMN retry_count   INT           NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_at TIMESTAMPTZ,
    ADD COLUMN last_error    VARCHAR(2000);

UPDATE outbox_events SET status = 'PUBLISHED' WHERE published_at IS NOT NULL;

ALTER TABLE outbox_events
    ADD CONSTRAINT ck_outbox_events_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    ADD CONSTRAINT ck_outbox_events_retry_count CHECK (retry_count >= 0);

DROP INDEX IF EXISTS idx_booking_outbox_pending;
CREATE INDEX idx_booking_outbox_status_created ON outbox_events (status, created_at);
CREATE INDEX idx_booking_outbox_aggregate_status ON outbox_events (aggregate_id, status);
