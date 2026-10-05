CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE movie_snapshots (
    movie_id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    duration_minutes INT NOT NULL,
    age_rating VARCHAR(20),
    status VARCHAR(30),
    version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE screen_snapshots (
    screen_id UUID PRIMARY KEY,
    cinema_id UUID NOT NULL,
    cinema_name VARCHAR(255) NOT NULL,
    screen_name VARCHAR(100) NOT NULL,
    screen_type VARCHAR(50),
    total_seats INT NOT NULL,
    status VARCHAR(30),
    version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE showtimes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL,
    screen_id UUID NOT NULL,
    movie_title VARCHAR(255) NOT NULL,
    cinema_id UUID NOT NULL,
    cinema_name VARCHAR(255) NOT NULL,
    screen_name VARCHAR(100) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    base_price NUMERIC(12,2) NOT NULL CHECK (base_price >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (end_time > start_time)
);

CREATE TABLE showtime_seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    showtime_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    seat_label VARCHAR(10) NOT NULL,
    seat_type VARCHAR(50),
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    locked_until TIMESTAMPTZ,
    UNIQUE (showtime_id, seat_id),
    CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED', 'BLOCKED'))
);

CREATE INDEX idx_showtimes_movie_time ON showtimes(movie_id, start_time);
CREATE INDEX idx_showtime_seats_status ON showtime_seats(showtime_id, status);
