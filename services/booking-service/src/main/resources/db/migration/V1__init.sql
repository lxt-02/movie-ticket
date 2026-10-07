CREATE TABLE bookings (
    id CHAR(36) PRIMARY KEY,
    booking_code VARCHAR(50) NOT NULL UNIQUE,
    user_id CHAR(36) NOT NULL,
    showtime_id CHAR(36) NOT NULL,
    movie_title VARCHAR(255) NOT NULL,
    cinema_name VARCHAR(255) NOT NULL,
    screen_name VARCHAR(100) NOT NULL,
    start_time DATETIME(6) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    confirmed_at DATETIME(6),
    cancelled_at DATETIME(6),
    CONSTRAINT bookings_status_check CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'REFUNDED'))
);

CREATE TABLE booking_seats (
    id CHAR(36) PRIMARY KEY,
    booking_id CHAR(36) NOT NULL,
    showtime_seat_id CHAR(36) NOT NULL,
    seat_id CHAR(36) NOT NULL,
    seat_label VARCHAR(10) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL CHECK (unit_price >= 0),
    ticket_code VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    UNIQUE (booking_id, showtime_seat_id)
);

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

CREATE INDEX idx_bookings_user_created ON bookings(user_id, created_at);
CREATE INDEX idx_bookings_expiry ON bookings(status, expires_at);
