-- Search (Elasticsearch) support:
--   1. age_rating limited to the Vietnamese classification, so search facets stay clean.
--   2. Outbox relay lock: with several movie-service instances only one publishes movie events at a time.
--   3. More seed data (genres, actors, Vietnamese titles) to demo multi-criteria search.

-- 1. Age rating: P, K, T13, T16, T18 (Vietnamese film classification) or NULL.
UPDATE movies SET age_rating = UPPER(TRIM(age_rating)) WHERE age_rating IS NOT NULL;
UPDATE movies SET age_rating = 'T13' WHERE age_rating = 'C13';
UPDATE movies SET age_rating = 'T16' WHERE age_rating = 'C16';
UPDATE movies SET age_rating = 'T18' WHERE age_rating = 'C18';
UPDATE movies SET age_rating = NULL WHERE age_rating NOT IN ('P', 'K', 'T13', 'T16', 'T18');
ALTER TABLE movies ADD CONSTRAINT ck_movies_age_rating
    CHECK (age_rating IS NULL OR age_rating IN ('P', 'K', 'T13', 'T16', 'T18'));

-- 2. Outbox relay
CREATE TABLE outbox_relay_lock (
    name VARCHAR(50) PRIMARY KEY
);
INSERT INTO outbox_relay_lock (name) VALUES ('movie-outbox');

CREATE INDEX idx_movie_outbox_pending ON outbox_events (published_at, created_at);

-- 3. Seed data
INSERT INTO genres (id, name) VALUES
    ('21000000-0000-0000-0000-000000000004', 'Action'),
    ('21000000-0000-0000-0000-000000000005', 'Drama'),
    ('21000000-0000-0000-0000-000000000006', 'Comedy'),
    ('21000000-0000-0000-0000-000000000007', 'Family'),
    ('21000000-0000-0000-0000-000000000008', 'Romance'),
    ('21000000-0000-0000-0000-000000000009', 'Horror');

INSERT INTO persons (id, name, avatar_url, biography) VALUES
    ('22000000-0000-0000-0000-000000000004', 'Sam Worthington', NULL, NULL),
    ('22000000-0000-0000-0000-000000000005', 'Zoe Saldaña', NULL, NULL),
    ('22000000-0000-0000-0000-000000000006', 'Timothée Chalamet', NULL, NULL),
    ('22000000-0000-0000-0000-000000000007', 'Zendaya', NULL, NULL),
    ('22000000-0000-0000-0000-000000000008', 'Amy Poehler', NULL, NULL),
    ('22000000-0000-0000-0000-000000000009', 'Lý Hải', NULL, 'Đạo diễn loạt phim Lật Mặt.'),
    ('22000000-0000-0000-0000-000000000010', 'Trấn Thành', NULL, 'Đạo diễn, diễn viên.'),
    ('22000000-0000-0000-0000-000000000011', 'Phương Anh Đào', NULL, NULL);

INSERT INTO movies (id, title, original_title, description, duration_minutes, release_date, age_rating,
                    language, country, poster_url, status) VALUES
    ('20000000-0000-0000-0000-000000000004',
     'Lật Mặt 7: Một Điều Ước',
     'Lật Mặt 7: Một Điều Ước',
     'Câu chuyện gia đình về người mẹ già và những người con trưởng thành mỗi người một ngả.',
     138, '2024-04-26', 'K', 'Vietnamese', 'Vietnam',
     'https://example.com/posters/lat-mat-7.jpg', 'NOW_SHOWING'),
    ('20000000-0000-0000-0000-000000000005',
     'Mai',
     'Mai',
     'Mai, một người phụ nữ làm nghề mát-xa, gặp Dương và dần mở lòng với một mối tình mới.',
     131, '2024-02-10', 'T18', 'Vietnamese', 'Vietnam',
     'https://example.com/posters/mai.jpg', 'NOW_SHOWING');

INSERT INTO movie_genres (movie_id, genre_id) VALUES
    ('20000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000004'),
    ('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000004'),
    ('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000005'),
    ('20000000-0000-0000-0000-000000000003', '21000000-0000-0000-0000-000000000006'),
    ('20000000-0000-0000-0000-000000000003', '21000000-0000-0000-0000-000000000007'),
    ('20000000-0000-0000-0000-000000000004', '21000000-0000-0000-0000-000000000005'),
    ('20000000-0000-0000-0000-000000000004', '21000000-0000-0000-0000-000000000007'),
    ('20000000-0000-0000-0000-000000000005', '21000000-0000-0000-0000-000000000005'),
    ('20000000-0000-0000-0000-000000000005', '21000000-0000-0000-0000-000000000008');

INSERT INTO movie_crew (movie_id, person_id, role, character_name) VALUES
    ('20000000-0000-0000-0000-000000000001', '22000000-0000-0000-0000-000000000004', 'ACTOR', 'Jake Sully'),
    ('20000000-0000-0000-0000-000000000001', '22000000-0000-0000-0000-000000000005', 'ACTOR', 'Neytiri'),
    ('20000000-0000-0000-0000-000000000002', '22000000-0000-0000-0000-000000000006', 'ACTOR', 'Paul Atreides'),
    ('20000000-0000-0000-0000-000000000002', '22000000-0000-0000-0000-000000000007', 'ACTOR', 'Chani'),
    ('20000000-0000-0000-0000-000000000003', '22000000-0000-0000-0000-000000000008', 'ACTOR', 'Joy'),
    ('20000000-0000-0000-0000-000000000004', '22000000-0000-0000-0000-000000000009', 'DIRECTOR', NULL),
    ('20000000-0000-0000-0000-000000000005', '22000000-0000-0000-0000-000000000010', 'DIRECTOR', NULL),
    ('20000000-0000-0000-0000-000000000005', '22000000-0000-0000-0000-000000000011', 'ACTOR', 'Mai');
