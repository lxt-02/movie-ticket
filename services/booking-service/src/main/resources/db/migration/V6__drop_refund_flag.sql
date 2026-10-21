-- No refunds in this system: drop the manual-refund flag and the old refund statuses.
ALTER TABLE bookings DROP INDEX idx_bookings_needs_refund;
ALTER TABLE bookings DROP COLUMN needs_refund, DROP COLUMN refund_note;

ALTER TABLE bookings DROP CHECK bookings_status_check;
ALTER TABLE bookings ADD CONSTRAINT bookings_status_check CHECK (
    status IN ('PENDING', 'CONFIRMING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')
);
