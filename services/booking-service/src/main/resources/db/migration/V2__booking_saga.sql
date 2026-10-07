ALTER TABLE bookings
    ADD COLUMN hold_id CHAR(36) UNIQUE,
    ADD COLUMN idempotency_key VARCHAR(150),
    ADD COLUMN request_hash VARCHAR(64),
    ADD COLUMN currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    ADD COLUMN paid_payment_id CHAR(36),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD CONSTRAINT uq_booking_user_request UNIQUE (user_id, idempotency_key),
    ADD CONSTRAINT ck_booking_request_pair CHECK (
        (idempotency_key IS NULL AND request_hash IS NULL)
        OR (idempotency_key IS NOT NULL AND request_hash IS NOT NULL)
    );

ALTER TABLE bookings DROP CHECK bookings_status_check;
ALTER TABLE bookings ADD CONSTRAINT bookings_status_check CHECK (
    status IN ('PENDING', 'CONFIRMING', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'REFUND_PENDING', 'REFUNDED')
);

CREATE INDEX idx_booking_outbox_pending ON outbox_events(published_at, created_at);
