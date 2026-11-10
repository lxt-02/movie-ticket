-- =====================================================================================================
-- payment-service demo data: one PayOS payment per seeded booking (booking-service V2__seed_data.sql).
-- No PENDING payment is seeded (the reconcile job would ask PayOS about it).
--   80000000-...-0001  BK10000001  SUCCESS    240.000
--   80000000-...-0002  BK10000002  SUCCESS    270.000
--   80000000-...-0003  BK10000003  CANCELLED  180.000  (khách huỷ đơn)
--   80000000-...-0004  BK10000004  EXPIRED    180.000  (không thanh toán kịp)
--   80000000-...-0005  BK10000005  SUCCESS    540.000  (staff)
-- =====================================================================================================

CREATE TEMP TABLE seed_day ON COMMIT DROP AS
SELECT (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS today;

CREATE TEMP TABLE seed_payment (
    payment_id UUID, booking_id UUID, user_id UUID, order_code BIGINT, status TEXT, amount NUMERIC,
    created_at TIMESTAMPTZ, failure_reason TEXT
) ON COMMIT DROP;

-- created_at = booking created_at + 10 seconds (same expressions as booking-service seed)
INSERT INTO seed_payment VALUES
    ('80000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
     1000000000001, 'SUCCESS', 240000.00,
     (((SELECT today FROM seed_day) - 4 + TIME '21:00') AT TIME ZONE 'Asia/Ho_Chi_Minh') + INTERVAL '10 seconds', NULL),
    ('80000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001',
     1000000000002, 'SUCCESS', 270000.00,
     date_trunc('minute', now()) - INTERVAL '1 hour' + INTERVAL '10 seconds', NULL),
    ('80000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001',
     1000000000003, 'CANCELLED', 180000.00,
     date_trunc('minute', now()) - INTERVAL '3 hours' + INTERVAL '10 seconds', 'Khách huỷ đơn đặt vé'),
    ('80000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001',
     1000000000004, 'EXPIRED', 180000.00,
     date_trunc('minute', now()) - INTERVAL '2 hours' + INTERVAL '10 seconds', 'Link thanh toán đã hết hạn'),
    ('80000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000002',
     1000000000005, 'SUCCESS', 540000.00,
     date_trunc('minute', now()) - INTERVAL '5 hours' + INTERVAL '10 seconds', NULL);

INSERT INTO payments (id, booking_id, user_id, payment_code, payment_method, payment_status, amount, currency,
                      checkout_url, qr_code, gateway_provider, payos_order_code, gateway_transaction_id,
                      idempotency_key, failure_reason, paid_at, expired_at, created_at, updated_at)
SELECT p.payment_id, p.booking_id, p.user_id,
       'PAY-' || p.order_code, 'PAYOS', p.status, p.amount, 'VND',
       NULL, NULL, 'PAYOS', p.order_code,
       CASE WHEN p.status = 'SUCCESS' THEN 'SEED-TX-' || p.order_code END,
       'payment:create:PAYOS:' || p.booking_id || ':1',
       p.failure_reason,
       CASE WHEN p.status = 'SUCCESS' THEN p.created_at + INTERVAL '90 seconds' END,
       -- link deadline = booking expires_at (booking created_at + 7 minutes)
       p.created_at - INTERVAL '10 seconds' + INTERVAL '7 minutes',
       p.created_at,
       p.created_at + CASE p.status WHEN 'SUCCESS' THEN INTERVAL '90 seconds'
                                    WHEN 'CANCELLED' THEN INTERVAL '170 seconds'
                                    ELSE INTERVAL '8 minutes' END
FROM seed_payment p;

INSERT INTO payment_transactions (id, payment_id, transaction_code, gateway_transaction_id, transaction_type,
                                  transaction_status, amount, raw_response, created_at)
SELECT md5('seed-tx:' || p.payment_id)::uuid, p.payment_id, 'PAY-' || p.order_code, 'SEED-TX-' || p.order_code,
       'PAYMENT', 'SUCCESS', p.amount,
       jsonb_build_object('source', 'seed', 'orderCode', p.order_code, 'amount', p.amount, 'code', '00'),
       p.created_at + INTERVAL '90 seconds'
FROM seed_payment p
WHERE p.status = 'SUCCESS';
