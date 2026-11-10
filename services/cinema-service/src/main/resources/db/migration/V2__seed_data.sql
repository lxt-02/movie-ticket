-- =====================================================================================================
-- cinema-service demo data: 4 cinemas, 8 screens, 55–133 seats per screen.
--
-- Seat ids are deterministic:  md5('seat:' || screen_id || ':' || label)::uuid
-- showtime-service and booking-service seeds rebuild the SAME ids with the SAME layout table below,
-- so if you change a layout here, change it in showtime-service V2__seed_data.sql too.
--
--   Cinema                         Screen          Type  Layout   Status
--   MV Cinema Landmark 81  (HCM)   Phòng 1         2D    STD90    ACTIVE   (A8 hỏng)
--                                  Phòng 2         3D    SMALL55  ACTIVE
--                                  IMAX            IMAX  IMAX133  ACTIVE
--   MV Cinema Vincom Đồng Khởi     Phòng 1         2D    STD90    ACTIVE   (C1, C2 hỏng)
--                                  Phòng 2         2D    SMALL55  MAINTENANCE  -> demo "sửa sơ đồ ghế"
--   MV Cinema Royal City (HN)      Phòng 1         2D    STD90    ACTIVE   (G12 hỏng)
--                                  Phòng 2         3D    SMALL55  ACTIVE
--   MV Cinema Đà Nẵng              Phòng 1         2D    STD90    ACTIVE   (rạp INACTIVE -> không có suất)
-- =====================================================================================================

INSERT INTO cinemas (id, name, brand, address, city, district, phone, status) VALUES
    ('30000000-0000-0000-0000-000000000001', 'MV Cinema Landmark 81', 'MV Cinema',
     'Tầng B1, Landmark 81, 720A Điện Biên Phủ, Phường 22', 'TP. Hồ Chí Minh', 'Bình Thạnh', '028 3000 0001', 'ACTIVE'),
    ('30000000-0000-0000-0000-000000000002', 'MV Cinema Vincom Đồng Khởi', 'MV Cinema',
     'Tầng 3, Vincom Center, 72 Lê Thánh Tôn, Phường Bến Nghé', 'TP. Hồ Chí Minh', 'Quận 1', '028 3000 0002', 'ACTIVE'),
    ('30000000-0000-0000-0000-000000000003', 'MV Cinema Royal City', 'MV Cinema',
     'Tầng B2, Vincom Mega Mall Royal City, 72A Nguyễn Trãi', 'Hà Nội', 'Thanh Xuân', '024 3000 0003', 'ACTIVE'),
    ('30000000-0000-0000-0000-000000000004', 'MV Cinema Đà Nẵng', 'MV Cinema',
     '255 Hùng Vương', 'Đà Nẵng', 'Hải Châu', '0236 3000 004', 'INACTIVE');

INSERT INTO screen_types (id, name, description) VALUES
    ('31000000-0000-0000-0000-000000000001', '2D', 'Chiếu 2D tiêu chuẩn'),
    ('31000000-0000-0000-0000-000000000002', 'IMAX', 'Màn hình lớn, âm thanh cao cấp'),
    ('31000000-0000-0000-0000-000000000003', '3D', 'Chiếu 3D, phát kính tại quầy');

INSERT INTO seat_types (id, name, price_multiplier) VALUES
    ('33000000-0000-0000-0000-000000000001', 'STANDARD', 1.00),
    ('33000000-0000-0000-0000-000000000002', 'VIP', 1.50),
    ('33000000-0000-0000-0000-000000000003', 'COUPLE', 2.00);

INSERT INTO screens (id, cinema_id, screen_type_id, name, total_seats, status) VALUES
    ('32000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '31000000-0000-0000-0000-000000000001', 'Phòng 1', 0, 'ACTIVE'),
    ('32000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', '31000000-0000-0000-0000-000000000003', 'Phòng 2', 0, 'ACTIVE'),
    ('32000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000001', '31000000-0000-0000-0000-000000000002', 'IMAX', 0, 'ACTIVE'),
    ('32000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000002', '31000000-0000-0000-0000-000000000001', 'Phòng 1', 0, 'ACTIVE'),
    ('32000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000002', '31000000-0000-0000-0000-000000000001', 'Phòng 2', 0, 'MAINTENANCE'),
    ('32000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000003', '31000000-0000-0000-0000-000000000001', 'Phòng 1', 0, 'ACTIVE'),
    ('32000000-0000-0000-0000-000000000007', '30000000-0000-0000-0000-000000000003', '31000000-0000-0000-0000-000000000003', 'Phòng 2', 0, 'ACTIVE'),
    ('32000000-0000-0000-0000-000000000008', '30000000-0000-0000-0000-000000000004', '31000000-0000-0000-0000-000000000001', 'Phòng 1', 0, 'ACTIVE');

-- ---------------------------------------------------------------- seat layouts (same table in showtime-service)
CREATE TEMP TABLE seed_layout (template TEXT, row_label TEXT, seat_count INT, seat_type TEXT) ON COMMIT DROP;
INSERT INTO seed_layout VALUES
    -- STD90: 3 hàng thường × 12, 4 hàng VIP × 12, 1 hàng ghế đôi × 6
    ('STD90', 'A', 12, 'STANDARD'), ('STD90', 'B', 12, 'STANDARD'), ('STD90', 'C', 12, 'STANDARD'),
    ('STD90', 'D', 12, 'VIP'), ('STD90', 'E', 12, 'VIP'), ('STD90', 'F', 12, 'VIP'), ('STD90', 'G', 12, 'VIP'),
    ('STD90', 'H', 6, 'COUPLE'),
    -- SMALL55: 2 hàng thường × 10, 3 hàng VIP × 10, 1 hàng ghế đôi × 5
    ('SMALL55', 'A', 10, 'STANDARD'), ('SMALL55', 'B', 10, 'STANDARD'),
    ('SMALL55', 'C', 10, 'VIP'), ('SMALL55', 'D', 10, 'VIP'), ('SMALL55', 'E', 10, 'VIP'),
    ('SMALL55', 'F', 5, 'COUPLE'),
    -- IMAX133: 4 hàng thường × 14, 5 hàng VIP × 14, 1 hàng ghế đôi × 7
    ('IMAX133', 'A', 14, 'STANDARD'), ('IMAX133', 'B', 14, 'STANDARD'), ('IMAX133', 'C', 14, 'STANDARD'), ('IMAX133', 'D', 14, 'STANDARD'),
    ('IMAX133', 'E', 14, 'VIP'), ('IMAX133', 'F', 14, 'VIP'), ('IMAX133', 'G', 14, 'VIP'), ('IMAX133', 'H', 14, 'VIP'), ('IMAX133', 'I', 14, 'VIP'),
    ('IMAX133', 'J', 7, 'COUPLE');

CREATE TEMP TABLE seed_screen (screen_id UUID, template TEXT) ON COMMIT DROP;
INSERT INTO seed_screen VALUES
    ('32000000-0000-0000-0000-000000000001', 'STD90'),
    ('32000000-0000-0000-0000-000000000002', 'SMALL55'),
    ('32000000-0000-0000-0000-000000000003', 'IMAX133'),
    ('32000000-0000-0000-0000-000000000004', 'STD90'),
    ('32000000-0000-0000-0000-000000000005', 'SMALL55'),
    ('32000000-0000-0000-0000-000000000006', 'STD90'),
    ('32000000-0000-0000-0000-000000000007', 'SMALL55'),
    ('32000000-0000-0000-0000-000000000008', 'STD90');

CREATE TEMP TABLE seed_broken (screen_id UUID, label TEXT) ON COMMIT DROP;
INSERT INTO seed_broken VALUES
    ('32000000-0000-0000-0000-000000000001', 'A8'),
    ('32000000-0000-0000-0000-000000000004', 'C1'),
    ('32000000-0000-0000-0000-000000000004', 'C2'),
    ('32000000-0000-0000-0000-000000000006', 'G12');

INSERT INTO seats (id, screen_id, seat_type_id, row_label, seat_number, label, status)
SELECT md5('seat:' || s.screen_id || ':' || l.row_label || n)::uuid,
       s.screen_id,
       st.id,
       l.row_label,
       n,
       l.row_label || n,
       CASE WHEN b.label IS NULL THEN 'ACTIVE' ELSE 'BROKEN' END
FROM seed_screen s
JOIN seed_layout l ON l.template = s.template
JOIN seat_types st ON st.name = l.seat_type
CROSS JOIN LATERAL generate_series(1, l.seat_count) AS n
LEFT JOIN seed_broken b ON b.screen_id = s.screen_id AND b.label = l.row_label || n;

UPDATE screens SET total_seats = (SELECT COUNT(*) FROM seats WHERE seats.screen_id = screens.id);
