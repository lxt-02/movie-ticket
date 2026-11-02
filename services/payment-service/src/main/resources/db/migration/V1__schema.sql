-- payment-service schema (PostgreSQL), PayOS + Kafka (spec: rule/payment/payment_service_payos.md).
-- Every id is a native UUID; times are TIMESTAMPTZ (java.time.Instant).

CREATE TABLE payments (
    id                     UUID          PRIMARY KEY,
    booking_id             UUID          NOT NULL,
    user_id                UUID          NOT NULL,
    payment_code           VARCHAR(64)   NOT NULL,
    payment_method         VARCHAR(32)   NOT NULL,
    payment_status         VARCHAR(32)   NOT NULL,
    amount                 NUMERIC(19,2) NOT NULL,
    currency               VARCHAR(3)    NOT NULL,
    checkout_url           VARCHAR(1024),
    qr_code                VARCHAR(1024),
    gateway_provider       VARCHAR(32)   NOT NULL,
    -- PayOS orderCode: a number by PayOS contract (not an id of ours).
    payos_order_code       BIGINT,
    gateway_transaction_id VARCHAR(255),
    idempotency_key        VARCHAR(255),
    failure_reason         VARCHAR(1000),
    paid_at                TIMESTAMPTZ,
    expired_at             TIMESTAMPTZ,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payments_payment_code UNIQUE (payment_code),
    -- UNIQUE allows many NULLs in PostgreSQL too.
    CONSTRAINT uk_payments_payos_order_code UNIQUE (payos_order_code),
    CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_payments_status CHECK (payment_status IN
        ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX idx_payments_booking_id ON payments (booking_id, created_at);
CREATE INDEX idx_payments_user_id ON payments (user_id);
CREATE INDEX idx_payments_status_expired_at ON payments (payment_status, expired_at);

-- Every PayOS callback / reconcile result, stored once.
CREATE TABLE payment_transactions (
    id                     UUID          PRIMARY KEY,
    payment_id             UUID          NOT NULL,
    transaction_code       VARCHAR(255)  NOT NULL,
    gateway_transaction_id VARCHAR(255)  NOT NULL,
    transaction_type       VARCHAR(32)   NOT NULL,
    transaction_status     VARCHAR(32)   NOT NULL,
    amount                 NUMERIC(19,2) NOT NULL,
    raw_response           JSONB,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_transactions_payment
        FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE,
    CONSTRAINT uk_payment_transactions_gateway_reference
        UNIQUE (payment_id, transaction_code, gateway_transaction_id),
    CONSTRAINT ck_payment_transactions_amount_non_negative CHECK (amount >= 0)
);

-- Transactional outbox: PAYMENT_* events (topic payment-events).
CREATE TABLE outbox_messages (
    id             UUID          PRIMARY KEY,
    aggregate_type VARCHAR(100)  NOT NULL,
    aggregate_id   UUID          NOT NULL,
    event_type     VARCHAR(150)  NOT NULL,
    topic          VARCHAR(255)  NOT NULL,
    message_key    VARCHAR(255)  NOT NULL,
    payload        JSONB         NOT NULL,
    status         VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    retry_count    INT           NOT NULL DEFAULT 0,
    next_retry_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at   TIMESTAMPTZ,
    last_error     VARCHAR(2000),
    CONSTRAINT ck_outbox_messages_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_outbox_messages_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_outbox_messages_status_next_retry_at ON outbox_messages (status, next_retry_at);
CREATE INDEX idx_outbox_messages_aggregate ON outbox_messages (aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_messages_created_at ON outbox_messages (created_at);

-- Reserved (commands are deduplicated by idempotency key).
CREATE TABLE inbox_events (
    event_id     UUID         PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
