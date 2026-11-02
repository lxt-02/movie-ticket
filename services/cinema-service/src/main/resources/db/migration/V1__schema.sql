-- cinema-service schema (PostgreSQL). Every id is a native UUID; times are TIMESTAMPTZ (java.time.Instant).

CREATE TABLE cinemas (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    brand      VARCHAR(100),
    address    TEXT         NOT NULL,
    city       VARCHAR(100) NOT NULL,
    district   VARCHAR(100),
    phone      VARCHAR(30),
    status     VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE screen_types (
    id          UUID        PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE screens (
    id             UUID         PRIMARY KEY,
    cinema_id      UUID         NOT NULL,
    screen_type_id UUID         NOT NULL,
    name           VARCHAR(100) NOT NULL,
    total_seats    INT          NOT NULL DEFAULT 0,
    status         VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uq_screens_cinema_name UNIQUE (cinema_id, name),
    CONSTRAINT fk_screens_cinema FOREIGN KEY (cinema_id) REFERENCES cinemas (id),
    CONSTRAINT fk_screens_screen_type FOREIGN KEY (screen_type_id) REFERENCES screen_types (id)
);

CREATE TABLE seat_types (
    id               UUID         PRIMARY KEY,
    name             VARCHAR(50)  NOT NULL UNIQUE,
    price_multiplier NUMERIC(5,2) NOT NULL DEFAULT 1.00,
    CONSTRAINT ck_seat_types_multiplier CHECK (price_multiplier > 0)
);

CREATE TABLE seats (
    id           UUID        PRIMARY KEY,
    screen_id    UUID        NOT NULL,
    seat_type_id UUID        NOT NULL,
    row_label    VARCHAR(5)  NOT NULL,
    seat_number  INT         NOT NULL,
    label        VARCHAR(10) NOT NULL,
    status       VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uq_seats_screen_label UNIQUE (screen_id, label),
    CONSTRAINT uq_seats_screen_position UNIQUE (screen_id, row_label, seat_number),
    CONSTRAINT fk_seats_screen FOREIGN KEY (screen_id) REFERENCES screens (id),
    CONSTRAINT fk_seats_seat_type FOREIGN KEY (seat_type_id) REFERENCES seat_types (id)
);

CREATE INDEX idx_screens_cinema ON screens (cinema_id);
CREATE INDEX idx_seats_screen ON seats (screen_id);

-- Reserved for cinema/screen events (not published yet).
CREATE TABLE outbox_events (
    id           UUID         PRIMARY KEY,
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB        NOT NULL,
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
