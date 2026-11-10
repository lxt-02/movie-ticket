-- =====================================================================================================
-- user-service demo profiles.
--   10000000-...-0001..0004 = the 4 Keycloak accounts of infras/keycloak/mss301-realm.json (same id = token "sub",
--   same email). Password of all of them: 123456.
--       customer  CUSTOMER  ACTIVE   -> đặt vé, có sẵn lịch sử vé
--       staff     STAFF     ACTIVE   -> trang vận hành
--       blocked   CUSTOMER  BLOCKED  -> đăng nhập được nhưng đặt vé bị 403
--       admin     ADMIN     ACTIVE   -> trang quản trị
--   10000000-...-0101.. are extra profiles WITHOUT a Keycloak account (only to fill the admin user list).
-- =====================================================================================================

INSERT INTO users (id, email, full_name, avatar_url, role, status, email_verified) VALUES
    ('10000000-0000-0000-0000-000000000001', 'customer.demo@example.com', 'Nguyễn Minh Khách', NULL, 'CUSTOMER', 'ACTIVE',  TRUE),
    ('10000000-0000-0000-0000-000000000002', 'staff.demo@example.com',    'Trần Thu Nhân',     NULL, 'STAFF',    'ACTIVE',  TRUE),
    ('10000000-0000-0000-0000-000000000003', 'blocked.demo@example.com',  'Lê Văn Khoá',       NULL, 'CUSTOMER', 'BLOCKED', TRUE),
    ('10000000-0000-0000-0000-000000000004', 'admin.demo@example.com',    'Phạm Quản Trị',     NULL, 'ADMIN',    'ACTIVE',  TRUE),
    ('10000000-0000-0000-0000-000000000101', 'an.nguyen@example.com',     'Nguyễn Thị An',     NULL, 'CUSTOMER', 'ACTIVE',  TRUE),
    ('10000000-0000-0000-0000-000000000102', 'binh.tran@example.com',     'Trần Văn Bình',     NULL, 'CUSTOMER', 'ACTIVE',  TRUE),
    ('10000000-0000-0000-0000-000000000103', 'chi.le@example.com',        'Lê Thu Chi',        NULL, 'CUSTOMER', 'ACTIVE',  FALSE),
    ('10000000-0000-0000-0000-000000000104', 'dung.pham@example.com',     'Phạm Quốc Dũng',    NULL, 'CUSTOMER', 'BLOCKED', TRUE),
    ('10000000-0000-0000-0000-000000000105', 'em.hoang@example.com',      'Hoàng Ngọc Em',     NULL, 'CUSTOMER', 'ACTIVE',  TRUE),
    ('10000000-0000-0000-0000-000000000106', 'giang.vo@example.com',      'Võ Trường Giang',   NULL, 'STAFF',    'ACTIVE',  TRUE);
