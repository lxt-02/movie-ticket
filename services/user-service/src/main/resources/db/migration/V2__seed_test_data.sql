INSERT INTO users (
    id,
    email,
    full_name,
    avatar_url,
    role,
    status,
    email_verified
) VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'customer.demo@example.com',
        'Demo Customer',
        'https://example.com/avatars/customer-demo.png',
        'CUSTOMER',
        'ACTIVE',
        TRUE
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'staff.demo@example.com',
        'Demo Staff',
        'https://example.com/avatars/staff-demo.png',
        'STAFF',
        'ACTIVE',
        TRUE
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'blocked.demo@example.com',
        'Blocked Demo Customer',
        'https://example.com/avatars/blocked-demo.png',
        'CUSTOMER',
        'BLOCKED',
        TRUE
    );
