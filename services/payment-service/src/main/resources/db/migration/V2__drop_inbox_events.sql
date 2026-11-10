-- payment-service does not use an inbox: payment commands are deduplicated by idempotencyKey on payments.
DROP TABLE IF EXISTS inbox_events;
