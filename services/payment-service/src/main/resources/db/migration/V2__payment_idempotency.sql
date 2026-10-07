ALTER TABLE payments
    ADD COLUMN idempotency_key VARCHAR(150),
    ADD COLUMN request_hash VARCHAR(64),
    ADD COLUMN expires_at DATETIME(6),
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD CONSTRAINT uq_payment_user_request UNIQUE (user_id, idempotency_key),
    ADD CONSTRAINT ck_payment_request_pair CHECK (
        (idempotency_key IS NULL AND request_hash IS NULL)
        OR (idempotency_key IS NOT NULL AND request_hash IS NOT NULL)
    );

ALTER TABLE payments DROP CHECK payments_status_check;
ALTER TABLE payments ADD CONSTRAINT payments_status_check CHECK (
    status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUND_PENDING', 'REFUNDED')
);

CREATE INDEX idx_payments_booking ON payments(booking_id, created_at);

ALTER TABLE refunds
    ADD COLUMN idempotency_key VARCHAR(150),
    ADD COLUMN request_hash VARCHAR(64),
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD CONSTRAINT uq_refund_request UNIQUE (payment_id, idempotency_key),
    ADD CONSTRAINT ck_refund_request_pair CHECK (
        (idempotency_key IS NULL AND request_hash IS NULL)
        OR (idempotency_key IS NOT NULL AND request_hash IS NOT NULL)
    );

CREATE INDEX idx_refunds_payment ON refunds(payment_id);
CREATE INDEX idx_payment_outbox_pending ON outbox_events(published_at, created_at);
