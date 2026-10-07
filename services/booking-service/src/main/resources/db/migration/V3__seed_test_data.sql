INSERT INTO bookings (
    id,
    booking_code,
    user_id,
    showtime_id,
    movie_title,
    cinema_name,
    screen_name,
    start_time,
    subtotal,
    discount_amount,
    total_amount,
    status,
    expires_at,
    hold_id,
    idempotency_key,
    request_hash,
    currency
) VALUES
    (
        '60000000-0000-0000-0000-000000000001',
        'BK-SEED-0001',
        '10000000-0000-0000-0000-000000000001',
        '50000000-0000-0000-0000-000000000001',
        'Avatar: The Way of Water',
        'MV Cinema Landmark 81',
        'Screen 1',
        '2030-01-15 19:00:00.000000',
        200000.00,
        0.00,
        200000.00,
        'PENDING',
        '2030-01-15 18:45:00.000000',
        '70000000-0000-0000-0000-000000000001',
        'seed-booking-001',
        'seed-hash-booking-001',
        'VND'
    );

INSERT INTO booking_seats (
    id,
    booking_id,
    showtime_seat_id,
    seat_id,
    seat_label,
    unit_price,
    ticket_code,
    status
) VALUES
    (
        '61000000-0000-0000-0000-000000000001',
        '60000000-0000-0000-0000-000000000001',
        '51000000-0000-0000-0000-000000000001',
        '40000000-0000-0000-0000-000000000001',
        'A1',
        100000.00,
        'TICKET-SEED-0001-A1',
        'ACTIVE'
    ),
    (
        '61000000-0000-0000-0000-000000000002',
        '60000000-0000-0000-0000-000000000001',
        '51000000-0000-0000-0000-000000000002',
        '40000000-0000-0000-0000-000000000002',
        'A2',
        100000.00,
        'TICKET-SEED-0001-A2',
        'ACTIVE'
    );
