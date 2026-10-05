CREATE TABLE seat_holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE,
    showtime_id UUID NOT NULL REFERENCES showtimes(id),
    idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'HELD'
        CHECK (status IN ('HELD', 'CONFIRMED', 'RELEASED', 'EXPIRED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (expires_at > created_at),
    UNIQUE (id, showtime_id)
);

ALTER TABLE showtime_seats ADD COLUMN hold_id UUID;
ALTER TABLE showtime_seats ADD CONSTRAINT fk_seat_hold_showtime
    FOREIGN KEY (hold_id, showtime_id) REFERENCES seat_holds(id, showtime_id);

ALTER TABLE showtime_seats ADD CONSTRAINT ck_seat_hold_ownership CHECK (
    (status = 'HELD' AND hold_id IS NOT NULL AND locked_until IS NOT NULL)
    OR (status = 'BOOKED' AND hold_id IS NOT NULL AND locked_until IS NULL)
    OR (status IN ('AVAILABLE', 'BLOCKED') AND hold_id IS NULL AND locked_until IS NULL)
) NOT VALID;

ALTER TABLE showtime_seats ADD CONSTRAINT uq_showtime_seat_identity UNIQUE (id, showtime_id);

CREATE TABLE seat_hold_items (
    hold_id UUID NOT NULL,
    showtime_id UUID NOT NULL,
    showtime_seat_id UUID NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    PRIMARY KEY (hold_id, showtime_seat_id),
    FOREIGN KEY (hold_id, showtime_id) REFERENCES seat_holds(id, showtime_id),
    FOREIGN KEY (showtime_seat_id, showtime_id) REFERENCES showtime_seats(id, showtime_id)
);

CREATE INDEX idx_seat_holds_expiry ON seat_holds(expires_at) WHERE status = 'HELD';
CREATE INDEX idx_showtime_seats_hold ON showtime_seats(hold_id) WHERE hold_id IS NOT NULL;

CREATE TABLE inbox_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_showtime_outbox_pending ON outbox_events(created_at) WHERE published_at IS NULL;
