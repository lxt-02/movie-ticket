CREATE TABLE movie_snapshots (
    movie_id CHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    duration_minutes INT NOT NULL,
    age_rating VARCHAR(20),
    status VARCHAR(30),
    version INT NOT NULL DEFAULT 1,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE screen_snapshots (
    screen_id CHAR(36) PRIMARY KEY,
    cinema_id CHAR(36) NOT NULL,
    cinema_name VARCHAR(255) NOT NULL,
    screen_name VARCHAR(100) NOT NULL,
    screen_type VARCHAR(50),
    total_seats INT NOT NULL,
    status VARCHAR(30),
    version INT NOT NULL DEFAULT 1,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE showtimes (
    id CHAR(36) PRIMARY KEY,
    movie_id CHAR(36) NOT NULL,
    screen_id CHAR(36) NOT NULL,
    movie_title VARCHAR(255) NOT NULL,
    cinema_id CHAR(36) NOT NULL,
    cinema_name VARCHAR(255) NOT NULL,
    screen_name VARCHAR(100) NOT NULL,
    start_time DATETIME(6) NOT NULL,
    end_time DATETIME(6) NOT NULL,
    base_price DECIMAL(12,2) NOT NULL CHECK (base_price >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHECK (end_time > start_time)
);

CREATE TABLE showtime_seats (
    id CHAR(36) PRIMARY KEY,
    showtime_id CHAR(36) NOT NULL,
    seat_id CHAR(36) NOT NULL,
    seat_label VARCHAR(10) NOT NULL,
    seat_type VARCHAR(50),
    price DECIMAL(12,2) NOT NULL CHECK (price >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    locked_until DATETIME(6),
    UNIQUE (showtime_id, seat_id),
    CONSTRAINT fk_showtime_seats_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(id),
    CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED', 'BLOCKED'))
);

CREATE INDEX idx_showtimes_movie_time ON showtimes(movie_id, start_time);
CREATE INDEX idx_showtime_seats_status ON showtime_seats(showtime_id, status);
