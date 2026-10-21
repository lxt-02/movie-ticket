-- Money received on a payment link that was already closed (booking cancelled / expired / link replaced):
-- the "LATE PAID" case. No refund flow in this system: flagged here for manual handling.
ALTER TABLE payments ADD COLUMN needs_refund BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_payments_needs_refund ON payments(needs_refund);
