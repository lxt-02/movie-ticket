-- Demo admin, same id as user "admin" in the Keycloak realm (infras/keycloak/mss301-realm.json).
INSERT INTO users (id, email, full_name, avatar_url, role, status, email_verified)
VALUES ('10000000-0000-0000-0000-000000000004', 'admin.demo@example.com', 'Demo Admin', NULL, 'ADMIN', 'ACTIVE', TRUE);
