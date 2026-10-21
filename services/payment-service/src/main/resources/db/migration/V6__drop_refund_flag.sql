-- No refunds in this system: drop the manual-refund flag and the old refund statuses.
ALTER TABLE payments DROP INDEX idx_payments_needs_refund;
ALTER TABLE payments DROP COLUMN needs_refund;

ALTER TABLE payments DROP CHECK ck_payments_status;
ALTER TABLE payments ADD CONSTRAINT ck_payments_status CHECK (payment_status IN
    ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'EXPIRED', 'CANCELLED'));
