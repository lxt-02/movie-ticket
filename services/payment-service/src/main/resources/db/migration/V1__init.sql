CREATE TABLE payments (
    id CHAR(36) PRIMARY KEY,
    booking_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_transaction_id VARCHAR(150) UNIQUE,
    amount DECIMAL(12,2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_url TEXT,
    paid_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT payments_status_check CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED'))
);

CREATE TABLE payment_events (
    id CHAR(36) PRIMARY KEY,
    payment_id CHAR(36) NOT NULL,
    provider_event_id VARCHAR(150) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_payment_events_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);

CREATE TABLE refunds (
    id CHAR(36) PRIMARY KEY,
    payment_id CHAR(36) NOT NULL,
    amount DECIMAL(12,2) NOT NULL CHECK (amount >= 0),
    reason TEXT,
    provider_refund_id VARCHAR(150) UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6),
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
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
