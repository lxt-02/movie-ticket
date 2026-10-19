-- V3: rebuild the payment schema for PayOS + Kafka (spec: rule/payment/payment_service_payos.md).
-- Payments created before V3 came from the mock gateway (test data only), so the old
-- payments / payment_events / outbox_events tables are dropped and recreated.
-- refunds is kept; its foreign key is re-pointed to the new payments table.

ALTER TABLE refunds DROP FOREIGN KEY fk_refunds_payment;
DELETE FROM refunds;

DROP TABLE IF EXISTS payment_events;
DROP TABLE IF EXISTS outbox_events;
DROP TABLE IF EXISTS payments;

CREATE TABLE payments
(
    id                     CHAR(36)       NOT NULL PRIMARY KEY,
    booking_id             CHAR(36)       NOT NULL,
    user_id                CHAR(36)       NOT NULL,
    payment_code           VARCHAR(64)    NOT NULL,
    payment_method         VARCHAR(32)    NOT NULL,
    payment_status         VARCHAR(32)    NOT NULL,
    amount                 DECIMAL(19, 2) NOT NULL,
    currency               VARCHAR(3)     NOT NULL,
    checkout_url           VARCHAR(1024),
    qr_code                VARCHAR(1024),
    gateway_provider       VARCHAR(32)    NOT NULL,
    payos_order_code       BIGINT,
    gateway_transaction_id VARCHAR(255),
    idempotency_key        VARCHAR(255),
    failure_reason         VARCHAR(1000),
    paid_at                DATETIME(6),
    expired_at             DATETIME(6),
    created_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_payments_payment_code UNIQUE (payment_code),
    -- MySQL UNIQUE allows many NULLs, equivalent to the partial unique indexes in the spec.
    CONSTRAINT uk_payments_payos_order_code UNIQUE (payos_order_code),
    CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_payments_status CHECK (payment_status IN
        ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'EXPIRED', 'CANCELLED', 'REFUND_PENDING', 'REFUNDED'))
);

CREATE INDEX idx_payments_booking_id ON payments (booking_id, created_at);
CREATE INDEX idx_payments_user_id ON payments (user_id);
CREATE INDEX idx_payments_status_expired_at ON payments (payment_status, expired_at);

CREATE TABLE payment_transactions
(
    id                     CHAR(36)       NOT NULL PRIMARY KEY,
    payment_id             CHAR(36)       NOT NULL,
    transaction_code       VARCHAR(255)   NOT NULL,
    gateway_transaction_id VARCHAR(255)   NOT NULL,
    transaction_type       VARCHAR(32)    NOT NULL,
    transaction_status     VARCHAR(32)    NOT NULL,
    amount                 DECIMAL(19, 2) NOT NULL,
    raw_response           JSON,
    created_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_payment_transactions_payment
        FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE,
    CONSTRAINT uk_payment_transactions_gateway_reference
        UNIQUE (payment_id, transaction_code, gateway_transaction_id),
    CONSTRAINT ck_payment_transactions_amount_non_negative CHECK (amount >= 0)
);

CREATE TABLE outbox_messages
(
    id             CHAR(36)      NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(100)  NOT NULL,
    aggregate_id   CHAR(36)      NOT NULL,
    event_type     VARCHAR(150)  NOT NULL,
    topic          VARCHAR(255)  NOT NULL,
    message_key    VARCHAR(255)  NOT NULL,
    payload        JSON          NOT NULL,
    status         VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    retry_count    INT           NOT NULL DEFAULT 0,
    next_retry_at  DATETIME(6),
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at   DATETIME(6),
    last_error     VARCHAR(2000),

    CONSTRAINT ck_outbox_messages_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_outbox_messages_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_outbox_messages_status_next_retry_at ON outbox_messages (status, next_retry_at);
CREATE INDEX idx_outbox_messages_aggregate ON outbox_messages (aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_messages_created_at ON outbox_messages (created_at);

ALTER TABLE refunds
    ADD CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id);
