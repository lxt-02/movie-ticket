CREATE TABLE seat_holds (
    id CHAR(36) PRIMARY KEY,
    booking_id CHAR(36) NOT NULL UNIQUE,
    showtime_id CHAR(36) NOT NULL,
    idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'HELD'
        CHECK (status IN ('HELD', 'CONFIRMED', 'RELEASED', 'EXPIRED')),
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE (id, showtime_id),
    CONSTRAINT fk_seat_holds_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(id),
    CHECK (expires_at > created_at)
);

ALTER TABLE showtime_seats ADD COLUMN hold_id CHAR(36);
ALTER TABLE showtime_seats ADD CONSTRAINT uq_showtime_seat_identity UNIQUE (id, showtime_id);
ALTER TABLE showtime_seats ADD CONSTRAINT fk_seat_hold_showtime
    FOREIGN KEY (hold_id, showtime_id) REFERENCES seat_holds(id, showtime_id);

ALTER TABLE showtime_seats ADD CONSTRAINT ck_seat_hold_ownership CHECK (
    (status = 'HELD' AND hold_id IS NOT NULL AND locked_until IS NOT NULL)
    OR (status = 'BOOKED' AND hold_id IS NOT NULL AND locked_until IS NULL)
    OR (status IN ('AVAILABLE', 'BLOCKED') AND hold_id IS NULL AND locked_until IS NULL)
);

CREATE TABLE seat_hold_items (
    hold_id CHAR(36) NOT NULL,
    showtime_id CHAR(36) NOT NULL,
    showtime_seat_id CHAR(36) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL CHECK (unit_price >= 0),
    PRIMARY KEY (hold_id, showtime_seat_id),
    CONSTRAINT fk_seat_hold_items_hold FOREIGN KEY (hold_id, showtime_id) REFERENCES seat_holds(id, showtime_id),
    CONSTRAINT fk_seat_hold_items_seat FOREIGN KEY (showtime_seat_id, showtime_id) REFERENCES showtime_seats(id, showtime_id)
);

CREATE INDEX idx_seat_holds_expiry ON seat_holds(status, expires_at);
CREATE INDEX idx_showtime_seats_hold ON showtime_seats(hold_id);

CREATE TABLE inbox_events (
    event_id CHAR(36) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE outbox_events (
    id CHAR(36) PRIMARY KEY,
    aggregate_id CHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    published_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_showtime_outbox_pending ON outbox_events(published_at, created_at);
