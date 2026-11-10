-- Outbox: outbox_events -> outbox_messages (same shape as booking-service / payment-service).
-- Each row carries its topic and Kafka key; publish errors are recorded (retry_count, last_error, next_retry_at).
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
    CONSTRAINT ck_movie_outbox_messages_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_movie_outbox_messages_retry_count CHECK (retry_count >= 0)
);

-- Existing rows: every movie event went to movie-events with key = movieId.
INSERT INTO outbox_messages (id, aggregate_type, aggregate_id, event_type, topic, message_key, payload,
                             status, retry_count, created_at, published_at)
SELECT id, 'MOVIE', aggregate_id, event_type, 'movie-events', CAST(aggregate_id AS VARCHAR(255)), payload,
       CASE WHEN published_at IS NULL THEN 'PENDING' ELSE 'PUBLISHED' END, 0, created_at, published_at
FROM outbox_events;

CREATE INDEX idx_movie_outbox_messages_status_created ON outbox_messages (status, created_at);
CREATE INDEX idx_movie_outbox_messages_aggregate ON outbox_messages (aggregate_type, aggregate_id);

DROP TABLE outbox_events;
