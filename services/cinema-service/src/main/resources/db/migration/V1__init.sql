CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE cinemas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(100),
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    phone VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE screen_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE screens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cinema_id UUID NOT NULL REFERENCES cinemas(id),
    screen_type_id UUID NOT NULL REFERENCES screen_types(id),
    name VARCHAR(100) NOT NULL,
    total_seats INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    UNIQUE (cinema_id, name)
);

CREATE TABLE seat_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    price_multiplier NUMERIC(5,2) NOT NULL DEFAULT 1.00 CHECK (price_multiplier > 0)
);

CREATE TABLE seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_id UUID NOT NULL REFERENCES screens(id),
    seat_type_id UUID NOT NULL REFERENCES seat_types(id),
    row_label VARCHAR(5) NOT NULL,
    seat_number INT NOT NULL,
    label VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    UNIQUE (screen_id, label),
    UNIQUE (screen_id, row_label, seat_number)
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
