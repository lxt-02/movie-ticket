-- 1) Money the booking cannot use (paid late, paid twice, wrong amount, seats lost after payment).
--    There is no refund flow: the flag makes these cases easy to list for manual handling
--    (GET /api/v1/bookings/admin/needs-refund) instead of being buried in the logs.
ALTER TABLE bookings
    ADD COLUMN needs_refund BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN refund_note VARCHAR(500);

CREATE INDEX idx_bookings_needs_refund ON bookings(needs_refund);

-- 2) One active outbox relay when several booking-service instances run (load balancing demo).
--    Each run locks this row with FOR UPDATE SKIP LOCKED; an instance that does not get it skips the run,
--    so commands are neither sent twice nor out of order.
CREATE TABLE outbox_relay_lock (
    name VARCHAR(50) PRIMARY KEY
);

INSERT INTO outbox_relay_lock (name) VALUES ('booking-outbox');
