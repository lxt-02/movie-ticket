-- booking-service schema (PostgreSQL). Every id is a native UUID; times are TIMESTAMPTZ (java.time.Instant).

CREATE TABLE bookings (
    id              UUID          PRIMARY KEY,
    booking_code    VARCHAR(50)   NOT NULL UNIQUE,
    user_id         UUID          NOT NULL,
    showtime_id     UUID          NOT NULL,
    movie_title     VARCHAR(255)  NOT NULL,
    cinema_name     VARCHAR(255)  NOT NULL,
    screen_name     VARCHAR(100)  NOT NULL,
    start_time      TIMESTAMPTZ   NOT NULL,
    subtotal        NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount    NUMERIC(12,2) NOT NULL DEFAULT 0,
    status          VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at    TIMESTAMPTZ,
    cancelled_at    TIMESTAMPTZ,
    -- Saga
    hold_id         UUID          UNIQUE,
    idempotency_key VARCHAR(150),
    request_hash    VARCHAR(64),
    currency        VARCHAR(10)   NOT NULL DEFAULT 'VND',
    paid_payment_id UUID,
    version         BIGINT        NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Payment links per booking (1 = first link; "pay again" increments it).
    payment_attempt INT           NOT NULL DEFAULT 1,
    CONSTRAINT uq_booking_user_request UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_booking_request_pair CHECK (
        (idempotency_key IS NULL AND request_hash IS NULL)
        OR (idempotency_key IS NOT NULL AND request_hash IS NOT NULL)
    ),
    CONSTRAINT bookings_status_check CHECK (status IN ('PENDING', 'CONFIRMING', 'CONFIRMED', 'CANCELLED', 'EXPIRED'))
);

CREATE INDEX idx_bookings_user_created ON bookings (user_id, created_at);
CREATE INDEX idx_bookings_expiry ON bookings (status, expires_at);
CREATE INDEX idx_bookings_status_updated ON bookings (status, updated_at);

CREATE TABLE booking_seats (
    id               UUID          PRIMARY KEY,
    booking_id       UUID          NOT NULL,
    showtime_seat_id UUID          NOT NULL,
    seat_id          UUID          NOT NULL,
    seat_label       VARCHAR(10)   NOT NULL,
    unit_price       NUMERIC(12,2) NOT NULL,
    ticket_code      VARCHAR(100)  NOT NULL UNIQUE,
    status           VARCHAR(30)   NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uq_booking_seats_seat UNIQUE (booking_id, showtime_seat_id),
    CONSTRAINT ck_booking_seats_price CHECK (unit_price >= 0)
);

-- Payment events already applied (Kafka redelivery is skipped).
CREATE TABLE inbox_events (
    event_id     UUID         PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Transactional outbox: CREATE_PAYMENT / CANCEL_PAYMENT commands (topic payment-commands).
CREATE TABLE outbox_events (
    id           UUID         PRIMARY KEY,
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB        NOT NULL,
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_booking_outbox_pending ON outbox_events (published_at, created_at);

-- One active outbox relay when several booking-service instances run (row locked with FOR UPDATE SKIP LOCKED),
-- so commands are neither sent twice nor out of order.
CREATE TABLE outbox_relay_lock (
    name VARCHAR(50) PRIMARY KEY
);
INSERT INTO outbox_relay_lock (name) VALUES ('booking-outbox');
