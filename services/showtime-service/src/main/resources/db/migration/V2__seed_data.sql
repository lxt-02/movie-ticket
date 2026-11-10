-- =====================================================================================================
-- showtime-service demo data. Times are computed from the moment the migration runs (Vietnam time), so a fresh
-- `docker compose down -v && docker compose up --build` always gives showtimes for "today" .. "today + 9".
--
-- * Grid: every ACTIVE screen of an ACTIVE cinema, 4 slots per day (09:30, 13:00, 16:30, 20:00 VN),
--   days 0..9. Slots that start in less than 30 minutes are skipped. Slots are 3h30 apart, longest movie is
--   192 min, so the 15-minute cleaning gap is always respected.
--   Movies rotate between the 7 NOW_SHOWING movies (IMAX: Avatar / Dune).
--   Base price: 2D 80.000, 3D 100.000, IMAX 140.000; +10.000 for the 20:00 slot; +10.000 on Sat/Sun.
--   Seat price = base price × seat type multiplier (STANDARD 1, VIP 1.5, COUPLE 2). Broken seats = BLOCKED.
--
-- * Two fixed showtimes (same ids as the old seed, used by the Postman collections in docs/ and by the
--   booking-service / payment-service seed):
--   50000000-...-0001  Dune, Landmark 81 Phòng 1, today+2 20:00 (SELLING)     -> customer: CONFIRMED (E7, E8)
--                                                                                 customer: CANCELLED (A5, A6)
--                                                                                 customer: EXPIRED   (B1, B2)
--                                                                                 staff:    CONFIRMED (G5..G8)
--      its seats keep the old id style: 51000000-0000-0000-0000-0000000000NN, NN = A1=1 .. A12=12, B1=13 ...
--      (A3 = ...0003, A4 = ...0004, A8 = ...0008 is the broken seat -> 409 test of the Postman collection)
--   50000000-...-0002  Mai, Landmark 81 Phòng 1, 3 days ago 19:30 (ENDED)      -> customer: CONFIRMED (D5, D6)
--      seats 51000000-0000-0000-0002-0000000000NN
--
-- Ids are deterministic and rebuilt identically by cinema-service (seats) and booking-service (seats):
--   seat           md5('seat:'     || screen_id   || ':' || label)::uuid
--   showtime_seat  md5('stseat:'   || showtime_id || ':' || label)::uuid   (except the 2 fixed showtimes above)
--   grid showtime  md5('showtime:' || screen_id   || ':' || day || ':' || slot)::uuid
-- =====================================================================================================

CREATE TEMP TABLE seed_day ON COMMIT DROP AS
SELECT (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS today;

-- ---------------------------------------------------------------- copies of movie-service / cinema-service data
CREATE TEMP TABLE seed_movie (no INT, movie_id UUID, title TEXT, duration INT, age_rating TEXT) ON COMMIT DROP;
INSERT INTO seed_movie VALUES
    (1, '20000000-0000-0000-0000-000000000001', 'Avatar: Dòng Chảy Của Nước', 192, 'T13'),
    (2, '20000000-0000-0000-0000-000000000002', 'Dune: Hành Tinh Cát – Phần Hai', 166, 'T16'),
    (3, '20000000-0000-0000-0000-000000000003', 'Những Mảnh Ghép Cảm Xúc 2', 96, 'P'),
    (4, '20000000-0000-0000-0000-000000000004', 'Lật Mặt 7: Một Điều Ước', 138, 'K'),
    (5, '20000000-0000-0000-0000-000000000005', 'Mai', 131, 'T18'),
    (6, '20000000-0000-0000-0000-000000000006', 'Doraemon: Nobita và Bản Giao Hưởng Địa Cầu', 115, 'P'),
    (7, '20000000-0000-0000-0000-000000000007', 'Đào, Phở và Piano', 100, 'T13');

CREATE TEMP TABLE seed_screen (
    no INT, screen_id UUID, template TEXT, cinema_id UUID, cinema_name TEXT, screen_name TEXT, screen_type TEXT,
    status TEXT, cinema_active BOOLEAN
) ON COMMIT DROP;
INSERT INTO seed_screen VALUES
    (1, '32000000-0000-0000-0000-000000000001', 'STD90',   '30000000-0000-0000-0000-000000000001', 'MV Cinema Landmark 81',      'Phòng 1', '2D',   'ACTIVE',      TRUE),
    (2, '32000000-0000-0000-0000-000000000002', 'SMALL55', '30000000-0000-0000-0000-000000000001', 'MV Cinema Landmark 81',      'Phòng 2', '3D',   'ACTIVE',      TRUE),
    (3, '32000000-0000-0000-0000-000000000003', 'IMAX133', '30000000-0000-0000-0000-000000000001', 'MV Cinema Landmark 81',      'IMAX',    'IMAX', 'ACTIVE',      TRUE),
    (4, '32000000-0000-0000-0000-000000000004', 'STD90',   '30000000-0000-0000-0000-000000000002', 'MV Cinema Vincom Đồng Khởi', 'Phòng 1', '2D',   'ACTIVE',      TRUE),
    (5, '32000000-0000-0000-0000-000000000005', 'SMALL55', '30000000-0000-0000-0000-000000000002', 'MV Cinema Vincom Đồng Khởi', 'Phòng 2', '2D',   'MAINTENANCE', TRUE),
    (6, '32000000-0000-0000-0000-000000000006', 'STD90',   '30000000-0000-0000-0000-000000000003', 'MV Cinema Royal City',       'Phòng 1', '2D',   'ACTIVE',      TRUE),
    (7, '32000000-0000-0000-0000-000000000007', 'SMALL55', '30000000-0000-0000-0000-000000000003', 'MV Cinema Royal City',       'Phòng 2', '3D',   'ACTIVE',      TRUE),
    (8, '32000000-0000-0000-0000-000000000008', 'STD90',   '30000000-0000-0000-0000-000000000004', 'MV Cinema Đà Nẵng',          'Phòng 1', '2D',   'ACTIVE',      FALSE);

-- same layout table as cinema-service V2__seed_data.sql
CREATE TEMP TABLE seed_layout (template TEXT, row_label TEXT, seat_count INT, seat_type TEXT) ON COMMIT DROP;
INSERT INTO seed_layout VALUES
    ('STD90', 'A', 12, 'STANDARD'), ('STD90', 'B', 12, 'STANDARD'), ('STD90', 'C', 12, 'STANDARD'),
    ('STD90', 'D', 12, 'VIP'), ('STD90', 'E', 12, 'VIP'), ('STD90', 'F', 12, 'VIP'), ('STD90', 'G', 12, 'VIP'),
    ('STD90', 'H', 6, 'COUPLE'),
    ('SMALL55', 'A', 10, 'STANDARD'), ('SMALL55', 'B', 10, 'STANDARD'),
    ('SMALL55', 'C', 10, 'VIP'), ('SMALL55', 'D', 10, 'VIP'), ('SMALL55', 'E', 10, 'VIP'),
    ('SMALL55', 'F', 5, 'COUPLE'),
    ('IMAX133', 'A', 14, 'STANDARD'), ('IMAX133', 'B', 14, 'STANDARD'), ('IMAX133', 'C', 14, 'STANDARD'), ('IMAX133', 'D', 14, 'STANDARD'),
    ('IMAX133', 'E', 14, 'VIP'), ('IMAX133', 'F', 14, 'VIP'), ('IMAX133', 'G', 14, 'VIP'), ('IMAX133', 'H', 14, 'VIP'), ('IMAX133', 'I', 14, 'VIP'),
    ('IMAX133', 'J', 7, 'COUPLE');

CREATE TEMP TABLE seed_seat_type (name TEXT, multiplier NUMERIC) ON COMMIT DROP;
INSERT INTO seed_seat_type VALUES ('STANDARD', 1.00), ('VIP', 1.50), ('COUPLE', 2.00);

CREATE TEMP TABLE seed_broken (screen_id UUID, label TEXT) ON COMMIT DROP;
INSERT INTO seed_broken VALUES
    ('32000000-0000-0000-0000-000000000001', 'A8'),
    ('32000000-0000-0000-0000-000000000004', 'C1'),
    ('32000000-0000-0000-0000-000000000004', 'C2'),
    ('32000000-0000-0000-0000-000000000006', 'G12');

CREATE TEMP TABLE seed_slot (slot_no INT, start_local TIME) ON COMMIT DROP;
INSERT INTO seed_slot VALUES (1, '09:30'), (2, '13:00'), (3, '16:30'), (4, '20:00');

-- ---------------------------------------------------------------- snapshots
INSERT INTO movie_snapshots (movie_id, title, duration_minutes, age_rating, status)
SELECT movie_id, title, duration, age_rating, 'NOW_SHOWING' FROM seed_movie;

INSERT INTO screen_snapshots (screen_id, cinema_id, cinema_name, screen_name, screen_type, total_seats, status)
SELECT s.screen_id, s.cinema_id, s.cinema_name, s.screen_name, s.screen_type,
       (SELECT SUM(l.seat_count) FROM seed_layout l WHERE l.template = s.template), s.status
FROM seed_screen s;

-- ---------------------------------------------------------------- fixed showtimes (booking history)
INSERT INTO showtimes (id, movie_id, screen_id, movie_title, cinema_id, cinema_name, screen_name,
                       start_time, end_time, base_price, status, created_at)
SELECT v.id, m.movie_id, s.screen_id, m.title, s.cinema_id, s.cinema_name, s.screen_name,
       v.start_time, v.start_time + make_interval(mins => m.duration), v.base_price, v.status, v.start_time - INTERVAL '10 days'
FROM (VALUES
        ('50000000-0000-0000-0000-000000000001'::uuid, 2, 1,
         ((SELECT today FROM seed_day) + 2 + TIME '20:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 90000.00, 'SELLING'),
        ('50000000-0000-0000-0000-000000000002'::uuid, 5, 1,
         ((SELECT today FROM seed_day) - 3 + TIME '19:30') AT TIME ZONE 'Asia/Ho_Chi_Minh', 80000.00, 'ENDED')
     ) AS v(id, movie_no, screen_no, start_time, base_price, status)
JOIN seed_movie m ON m.no = v.movie_no
JOIN seed_screen s ON s.no = v.screen_no;

-- ---------------------------------------------------------------- showtime grid
INSERT INTO showtimes (id, movie_id, screen_id, movie_title, cinema_id, cinema_name, screen_name,
                       start_time, end_time, base_price, status, created_at)
SELECT md5('showtime:' || s.screen_id || ':' || d || ':' || sl.slot_no)::uuid,
       m.movie_id, s.screen_id, m.title, s.cinema_id, s.cinema_name, s.screen_name,
       t.start_time,
       t.start_time + make_interval(mins => m.duration),
       (CASE s.screen_type WHEN 'IMAX' THEN 140000 WHEN '3D' THEN 100000 ELSE 80000 END)
         + (CASE WHEN sl.slot_no = 4 THEN 10000 ELSE 0 END)
         + (CASE WHEN EXTRACT(ISODOW FROM t.day) IN (6, 7) THEN 10000 ELSE 0 END),
       CASE WHEN t.start_time <= now() + INTERVAL '7 days' THEN 'SELLING' ELSE 'SCHEDULED' END,
       now()
FROM seed_screen s
CROSS JOIN generate_series(0, 9) AS d
CROSS JOIN seed_slot sl
CROSS JOIN LATERAL (
    SELECT (SELECT today FROM seed_day) + d AS day,
           ((SELECT today FROM seed_day) + d + sl.start_local) AT TIME ZONE 'Asia/Ho_Chi_Minh' AS start_time
) t
JOIN seed_movie m ON m.no = CASE
        WHEN s.screen_type = 'IMAX' THEN 1 + (d + sl.slot_no) % 2
        ELSE 1 + (s.no * 3 + d + sl.slot_no) % 7
    END
WHERE s.status = 'ACTIVE'
  AND s.cinema_active
  AND t.start_time > now() + INTERVAL '30 minutes'
  -- this slot is taken by fixed showtime 50000000-...-0001
  AND NOT (s.no = 1 AND d = 2 AND sl.slot_no = 4);

-- ---------------------------------------------------------------- seats of every showtime
INSERT INTO showtime_seats (id, showtime_id, seat_id, seat_label, seat_type, price, status, locked_until, hold_id)
SELECT CASE sh.id
           WHEN '50000000-0000-0000-0000-000000000001' THEN ('51000000-0000-0000-0000-' || lpad(
               (ROW_NUMBER() OVER (PARTITION BY sh.id ORDER BY l.row_label, n))::text, 12, '0'))::uuid
           WHEN '50000000-0000-0000-0000-000000000002' THEN ('51000000-0000-0000-0002-' || lpad(
               (ROW_NUMBER() OVER (PARTITION BY sh.id ORDER BY l.row_label, n))::text, 12, '0'))::uuid
           ELSE md5('stseat:' || sh.id || ':' || l.row_label || n)::uuid
       END,
       sh.id,
       md5('seat:' || sh.screen_id || ':' || l.row_label || n)::uuid,
       l.row_label || n,
       l.seat_type,
       ROUND(sh.base_price * st.multiplier, 2),
       CASE WHEN b.label IS NULL THEN 'AVAILABLE' ELSE 'BLOCKED' END,
       NULL,
       NULL
FROM showtimes sh
JOIN seed_screen s ON s.screen_id = sh.screen_id
JOIN seed_layout l ON l.template = s.template
JOIN seed_seat_type st ON st.name = l.seat_type
CROSS JOIN LATERAL generate_series(1, l.seat_count) AS n
LEFT JOIN seed_broken b ON b.screen_id = s.screen_id AND b.label = l.row_label || n;

-- ---------------------------------------------------------------- seat holds of the seeded bookings
CREATE TEMP TABLE seed_hold (
    hold_id UUID, booking_id UUID, showtime_id UUID, status TEXT, created_at TIMESTAMPTZ, labels TEXT[]
) ON COMMIT DROP;
INSERT INTO seed_hold VALUES
    ('70000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000002',
     'CONFIRMED', ((SELECT today FROM seed_day) - 4 + TIME '21:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', ARRAY['D5', 'D6']),
    ('70000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001',
     'CONFIRMED', date_trunc('minute', now()) - INTERVAL '1 hour',    ARRAY['E7', 'E8']),
    ('70000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000001',
     'RELEASED',  date_trunc('minute', now()) - INTERVAL '3 hours',   ARRAY['A5', 'A6']),
    ('70000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000004', '50000000-0000-0000-0000-000000000001',
     'EXPIRED',   date_trunc('minute', now()) - INTERVAL '2 hours',   ARRAY['B1', 'B2']),
    ('70000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000005', '50000000-0000-0000-0000-000000000001',
     'CONFIRMED', date_trunc('minute', now()) - INTERVAL '5 hours',   ARRAY['G5', 'G6', 'G7', 'G8']);

INSERT INTO seat_holds (id, booking_id, showtime_id, idempotency_key, request_hash, status, expires_at, created_at, updated_at)
SELECT hold_id, booking_id, showtime_id, 'booking:' || booking_id, md5('seed-hold:' || booking_id), status,
       created_at + INTERVAL '10 minutes', created_at, created_at + INTERVAL '2 minutes'
FROM seed_hold;

INSERT INTO seat_hold_items (hold_id, showtime_id, showtime_seat_id, unit_price)
SELECT h.hold_id, h.showtime_id, ss.id, ss.price
FROM seed_hold h
CROSS JOIN LATERAL unnest(h.labels) AS lbl
JOIN showtime_seats ss ON ss.showtime_id = h.showtime_id AND ss.seat_label = lbl;

-- paid bookings: seats sold
UPDATE showtime_seats ss
SET status = 'BOOKED', hold_id = h.hold_id, locked_until = NULL
FROM seed_hold h
WHERE h.status = 'CONFIRMED'
  AND ss.showtime_id = h.showtime_id
  AND ss.seat_label = ANY (h.labels);

