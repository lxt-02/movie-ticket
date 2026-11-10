-- Runs once, when the postgres container starts on an empty volume (docker-entrypoint-initdb.d).
-- One database per service (database-per-service); each service creates its own tables with Flyway.
-- To re-run it: docker compose down -v (wipes the data) and up again.
CREATE DATABASE user_db;
CREATE DATABASE movie_db;
CREATE DATABASE cinema_db;
CREATE DATABASE showtime_db;
CREATE DATABASE booking_db;
CREATE DATABASE payment_db;
