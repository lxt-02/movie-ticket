-- Payment attempt counter: each attempt sends CREATE_PAYMENT with key payment:create:PAYOS:{bookingId}:{attempt}.
ALTER TABLE bookings ADD COLUMN payment_attempt INT NOT NULL DEFAULT 1;
