-- =====================================================================================================
-- booking-service demo data: booking history that matches showtime-service (holds 70000000-...) and
-- payment-service (payments 80000000-...). No PENDING booking is seeded, so no seat stays held.
--
--   Booking     User      Showtime                         Seats        Status     Payment
--   BK10000001  customer  Mai, 3 days ago 19:30            D5 D6 (VIP)  CONFIRMED  SUCCESS   240.000
--   BK10000002  customer  Dune, today+2 20:00              E7 E8 (VIP)  CONFIRMED  SUCCESS   270.000
--   BK10000003  customer  Dune, today+2 20:00              A5 A6        CANCELLED  CANCELLED 180.000
--   BK10000004  customer  Dune, today+2 20:00              B1 B2        EXPIRED    EXPIRED   180.000
--   BK10000005  staff     Dune, today+2 20:00              G5..G8 (VIP) CONFIRMED  SUCCESS   540.000
--
-- Showtimes: 50000000-...-0001 = Dune today+2 20:00, 50000000-...-0002 = Mai 3 days ago (showtime-service seed).
-- Ids rebuilt like showtime-service: seat = md5('seat:'||screen||':'||label), showtime seat =
-- '51000000-0000-0000-000X-' || NN (NN = A1=1 .. A12=12, B1=13 ...). Times use the migration moment (Vietnam day).
-- =====================================================================================================

CREATE TEMP TABLE seed_day ON COMMIT DROP AS
SELECT (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS today;

CREATE TEMP TABLE seed_booking (
    no INT, booking_id UUID, code TEXT, user_id UUID, hold_id UUID, payment_id UUID,
    showtime_id UUID, screen_id UUID, movie_title TEXT, cinema_name TEXT, screen_name TEXT, start_time TIMESTAMPTZ,
    status TEXT, created_at TIMESTAMPTZ, labels TEXT[], unit_price NUMERIC
) ON COMMIT DROP;

INSERT INTO seed_booking VALUES
    (1, '60000000-0000-0000-0000-000000000001', 'BK10000001', '10000000-0000-0000-0000-000000000001',
     '70000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001',
     '50000000-0000-0000-0000-000000000002', '32000000-0000-0000-0000-000000000001',
     'Mai', 'MV Cinema Landmark 81', 'Phòng 1',
     ((SELECT today FROM seed_day) - 3 + TIME '19:30') AT TIME ZONE 'Asia/Ho_Chi_Minh',
     'CONFIRMED', ((SELECT today FROM seed_day) - 4 + TIME '21:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
     ARRAY['D5', 'D6'], 120000.00),
    (2, '60000000-0000-0000-0000-000000000002', 'BK10000002', '10000000-0000-0000-0000-000000000001',
     '70000000-0000-0000-0000-000000000002', '80000000-0000-0000-0000-000000000002',
     '50000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000001',
     'Dune: Hành Tinh Cát – Phần Hai', 'MV Cinema Landmark 81', 'Phòng 1',
     ((SELECT today FROM seed_day) + 2 + TIME '20:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
     'CONFIRMED', date_trunc('minute', now()) - INTERVAL '1 hour', ARRAY['E7', 'E8'], 135000.00),
    (3, '60000000-0000-0000-0000-000000000003', 'BK10000003', '10000000-0000-0000-0000-000000000001',
     '70000000-0000-0000-0000-000000000003', '80000000-0000-0000-0000-000000000003',
     '50000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000001',
     'Dune: Hành Tinh Cát – Phần Hai', 'MV Cinema Landmark 81', 'Phòng 1',
     ((SELECT today FROM seed_day) + 2 + TIME '20:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
     'CANCELLED', date_trunc('minute', now()) - INTERVAL '3 hours', ARRAY['A5', 'A6'], 90000.00),
    (4, '60000000-0000-0000-0000-000000000004', 'BK10000004', '10000000-0000-0000-0000-000000000001',
     '70000000-0000-0000-0000-000000000004', '80000000-0000-0000-0000-000000000004',
     '50000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000001',
     'Dune: Hành Tinh Cát – Phần Hai', 'MV Cinema Landmark 81', 'Phòng 1',
     ((SELECT today FROM seed_day) + 2 + TIME '20:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
     'EXPIRED', date_trunc('minute', now()) - INTERVAL '2 hours', ARRAY['B1', 'B2'], 90000.00),
    (5, '60000000-0000-0000-0000-000000000005', 'BK10000005', '10000000-0000-0000-0000-000000000002',
     '70000000-0000-0000-0000-000000000005', '80000000-0000-0000-0000-000000000005',
     '50000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000001',
     'Dune: Hành Tinh Cát – Phần Hai', 'MV Cinema Landmark 81', 'Phòng 1',
     ((SELECT today FROM seed_day) + 2 + TIME '20:00') AT TIME ZONE 'Asia/Ho_Chi_Minh',
     'CONFIRMED', date_trunc('minute', now()) - INTERVAL '5 hours', ARRAY['G5', 'G6', 'G7', 'G8'], 135000.00);

INSERT INTO bookings (id, booking_code, user_id, showtime_id, movie_title, cinema_name, screen_name, start_time,
                      subtotal, discount_amount, total_amount, status, expires_at, created_at, confirmed_at,
                      cancelled_at, hold_id, idempotency_key, request_hash, currency, paid_payment_id, version,
                      updated_at, payment_attempt)
SELECT b.booking_id, b.code, b.user_id, b.showtime_id, b.movie_title, b.cinema_name, b.screen_name, b.start_time,
       b.unit_price * cardinality(b.labels), 0, b.unit_price * cardinality(b.labels),
       b.status,
       b.created_at + INTERVAL '7 minutes',
       b.created_at,
       CASE WHEN b.status = 'CONFIRMED' THEN b.created_at + INTERVAL '2 minutes' END,
       CASE WHEN b.status = 'CANCELLED' THEN b.created_at + INTERVAL '3 minutes'
            WHEN b.status = 'EXPIRED' THEN b.created_at + INTERVAL '9 minutes' END,
       b.hold_id,
       'seed-' || b.code,
       md5('seed-request:' || b.booking_id),
       'VND',
       CASE WHEN b.status = 'CONFIRMED' THEN b.payment_id END,
       2,
       b.created_at + CASE b.status WHEN 'CONFIRMED' THEN INTERVAL '2 minutes'
                                    WHEN 'CANCELLED' THEN INTERVAL '3 minutes'
                                    ELSE INTERVAL '9 minutes' END,
       1
FROM seed_booking b;

INSERT INTO booking_seats (id, booking_id, showtime_seat_id, seat_id, seat_label, unit_price, ticket_code, status)
SELECT md5('booking-seat:' || b.booking_id || ':' || lbl)::uuid,
       b.booking_id,
       -- fixed showtimes keep the old id style (see showtime-service seed): row index × 12 + seat number
       ((CASE b.showtime_id WHEN '50000000-0000-0000-0000-000000000001' THEN '51000000-0000-0000-0000-'
                            ELSE '51000000-0000-0000-0002-' END)
        || lpad(((ascii(substr(lbl, 1, 1)) - 65) * 12 + substr(lbl, 2)::int)::text, 12, '0'))::uuid,
       md5('seat:' || b.screen_id || ':' || lbl)::uuid,
       lbl,
       b.unit_price,
       'TKT-' || upper(substr(md5('ticket:' || b.booking_id || ':' || lbl), 1, 12)),
       CASE b.status WHEN 'CONFIRMED' THEN 'ACTIVE' ELSE b.status END
FROM seed_booking b
CROSS JOIN LATERAL unnest(b.labels) AS lbl;
