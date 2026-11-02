-- showtime-service schema (PostgreSQL). Every id is a native UUID; times are TIMESTAMPTZ (java.time.Instant).

-- Local copies of movie / screen data (kept from movie-service and cinema-service).
CREATE TABLE movie_snapshots (
    movie_id         UUID         PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    duration_minutes INT          NOT NULL,
    age_rating       VARCHAR(20),
    status           VARCHAR(30),
    version          INT          NOT NULL DEFAULT 1,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE screen_snapshots (
    screen_id   UUID         PRIMARY KEY,
    cinema_id   UUID         NOT NULL,
    cinema_name VARCHAR(255) NOT NULL,
    screen_name VARCHAR(100) NOT NULL,
    screen_type VARCHAR(50),
    total_seats INT          NOT NULL,
    status      VARCHAR(30),
    version     INT          NOT NULL DEFAULT 1,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE showtimes (
    id          UUID          PRIMARY KEY,
    movie_id    UUID          NOT NULL,
    screen_id   UUID          NOT NULL,
    movie_title VARCHAR(255)  NOT NULL,
    cinema_id   UUID          NOT NULL,
    cinema_name VARCHAR(255)  NOT NULL,
    screen_name VARCHAR(100)  NOT NULL,
    start_time  TIMESTAMPTZ   NOT NULL,
    end_time    TIMESTAMPTZ   NOT NULL,
    base_price  NUMERIC(12,2) NOT NULL,
    status      VARCHAR(30)   NOT NULL DEFAULT 'SCHEDULED',
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_showtimes_price CHECK (base_price >= 0),
    CONSTRAINT ck_showtimes_time CHECK (end_time > start_time)
);

CREATE INDEX idx_showtimes_movie_time ON showtimes (movie_id, start_time);
CREATE INDEX idx_showtimes_screen_time ON showtimes (screen_id, start_time);

-- Seat holds: one per booking (booking_id UNIQUE = idempotency of the hold call).
CREATE TABLE seat_holds (
    id              UUID         PRIMARY KEY,
    booking_id      UUID         NOT NULL UNIQUE,
    showtime_id     UUID         NOT NULL,
    idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    request_hash    VARCHAR(64)  NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'HELD',
    expires_at      TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_seat_holds_identity UNIQUE (id, showtime_id),
    CONSTRAINT fk_seat_holds_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes (id),
    CONSTRAINT ck_seat_holds_status CHECK (status IN ('HELD', 'CONFIRMED', 'RELEASED', 'EXPIRED')),
    CONSTRAINT ck_seat_holds_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_seat_holds_expiry ON seat_holds (status, expires_at);
CREATE INDEX idx_seat_holds_showtime ON seat_holds (showtime_id);

CREATE TABLE showtime_seats (
    id           UUID          PRIMARY KEY,
    showtime_id  UUID          NOT NULL,
    seat_id      UUID          NOT NULL,
    seat_label   VARCHAR(10)   NOT NULL,
    seat_type    VARCHAR(50),
    price        NUMERIC(12,2) NOT NULL,
    status       VARCHAR(30)   NOT NULL DEFAULT 'AVAILABLE',
    locked_until TIMESTAMPTZ,
    hold_id      UUID,
    CONSTRAINT uq_showtime_seats_seat UNIQUE (showtime_id, seat_id),
    CONSTRAINT uq_showtime_seat_identity UNIQUE (id, showtime_id),
    CONSTRAINT fk_showtime_seats_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes (id),
    CONSTRAINT fk_seat_hold_showtime FOREIGN KEY (hold_id, showtime_id) REFERENCES seat_holds (id, showtime_id),
    CONSTRAINT ck_showtime_seats_price CHECK (price >= 0),
    CONSTRAINT ck_showtime_seats_status CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED', 'BLOCKED')),
    CONSTRAINT ck_seat_hold_ownership CHECK (
        (status = 'HELD' AND hold_id IS NOT NULL AND locked_until IS NOT NULL)
        OR (status = 'BOOKED' AND hold_id IS NOT NULL AND locked_until IS NULL)
        OR (status IN ('AVAILABLE', 'BLOCKED') AND hold_id IS NULL AND locked_until IS NULL)
    )
);

CREATE INDEX idx_showtime_seats_status ON showtime_seats (showtime_id, status);
CREATE INDEX idx_showtime_seats_hold ON showtime_seats (hold_id);

CREATE TABLE seat_hold_items (
    hold_id          UUID          NOT NULL,
    showtime_id      UUID          NOT NULL,
    showtime_seat_id UUID          NOT NULL,
    unit_price       NUMERIC(12,2) NOT NULL,
    PRIMARY KEY (hold_id, showtime_seat_id),
    CONSTRAINT fk_seat_hold_items_hold FOREIGN KEY (hold_id, showtime_id) REFERENCES seat_holds (id, showtime_id),
    CONSTRAINT fk_seat_hold_items_seat FOREIGN KEY (showtime_seat_id, showtime_id) REFERENCES showtime_seats (id, showtime_id),
    CONSTRAINT ck_seat_hold_items_price CHECK (unit_price >= 0)
);

CREATE INDEX idx_seat_hold_items_seat ON seat_hold_items (showtime_seat_id, showtime_id);

-- Reserved for showtime events (not used yet).
CREATE TABLE inbox_events (
    event_id     UUID         PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox_events (
    id           UUID         PRIMARY KEY,
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB        NOT NULL,
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_showtime_outbox_pending ON outbox_events (published_at, created_at);
