CREATE TABLE cinemas (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(100),
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    phone VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE screen_types (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE screens (
    id CHAR(36) PRIMARY KEY,
    cinema_id CHAR(36) NOT NULL,
    screen_type_id CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    total_seats INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    UNIQUE (cinema_id, name),
    CONSTRAINT fk_screens_cinema FOREIGN KEY (cinema_id) REFERENCES cinemas(id),
    CONSTRAINT fk_screens_screen_type FOREIGN KEY (screen_type_id) REFERENCES screen_types(id)
);

CREATE TABLE seat_types (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    price_multiplier DECIMAL(5,2) NOT NULL DEFAULT 1.00 CHECK (price_multiplier > 0)
);

CREATE TABLE seats (
    id CHAR(36) PRIMARY KEY,
    screen_id CHAR(36) NOT NULL,
    seat_type_id CHAR(36) NOT NULL,
    row_label VARCHAR(5) NOT NULL,
    seat_number INT NOT NULL,
    label VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    UNIQUE (screen_id, label),
    UNIQUE (screen_id, row_label, seat_number),
    CONSTRAINT fk_seats_screen FOREIGN KEY (screen_id) REFERENCES screens(id),
    CONSTRAINT fk_seats_seat_type FOREIGN KEY (seat_type_id) REFERENCES seat_types(id)
);

CREATE TABLE outbox_events (
    id CHAR(36) PRIMARY KEY,
    aggregate_id CHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    published_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
