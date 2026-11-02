-- Demo movies, genres, people and crew (Vietnamese titles included for the search demo).

INSERT INTO movies (
    id,
    title,
    original_title,
    description,
    duration_minutes,
    release_date,
    age_rating,
    language,
    country,
    poster_url,
    status
) VALUES
    (
        '20000000-0000-0000-0000-000000000001',
        'Avatar: The Way of Water',
        'Avatar: The Way of Water',
        'Jake Sully and Neytiri explore new regions of Pandora with their family.',
        192,
        '2022-12-16',
        'T13',
        'English',
        'United States',
        'https://example.com/posters/avatar-the-way-of-water.jpg',
        'NOW_SHOWING'
    ),
    (
        '20000000-0000-0000-0000-000000000002',
        'Dune: Part Two',
        'Dune: Part Two',
        'Paul Atreides joins the Fremen while seeking justice for House Atreides.',
        166,
        '2024-03-01',
        'T16',
        'English',
        'United States',
        'https://example.com/posters/dune-part-two.jpg',
        'NOW_SHOWING'
    ),
    (
        '20000000-0000-0000-0000-000000000003',
        'Inside Out 2',
        'Inside Out 2',
        'Riley grows up and meets new emotions.',
        96,
        '2024-06-14',
        'P',
        'English',
        'United States',
        'https://example.com/posters/inside-out-2.jpg',
        'COMING_SOON'
    );

INSERT INTO genres (id, name) VALUES
    ('21000000-0000-0000-0000-000000000001', 'Science Fiction'),
    ('21000000-0000-0000-0000-000000000002', 'Adventure'),
    ('21000000-0000-0000-0000-000000000003', 'Animation');

INSERT INTO movie_genres (movie_id, genre_id) VALUES
    ('20000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000001'),
    ('20000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000002'),
    ('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000001'),
    ('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000002'),
    ('20000000-0000-0000-0000-000000000003', '21000000-0000-0000-0000-000000000003');

INSERT INTO persons (id, name, avatar_url, biography) VALUES
    (
        '22000000-0000-0000-0000-000000000001',
        'James Cameron',
        'https://example.com/people/james-cameron.jpg',
        'Director and writer known for large-scale science fiction films.'
    ),
    (
        '22000000-0000-0000-0000-000000000002',
        'Denis Villeneuve',
        'https://example.com/people/denis-villeneuve.jpg',
        'Director known for visually rich science fiction and drama films.'
    ),
    (
        '22000000-0000-0000-0000-000000000003',
        'Kelsey Mann',
        'https://example.com/people/kelsey-mann.jpg',
        'Animation director.'
    );

INSERT INTO movie_crew (movie_id, person_id, role, character_name) VALUES
    ('20000000-0000-0000-0000-000000000001', '22000000-0000-0000-0000-000000000001', 'DIRECTOR', NULL),
    ('20000000-0000-0000-0000-000000000002', '22000000-0000-0000-0000-000000000002', 'DIRECTOR', NULL),
    ('20000000-0000-0000-0000-000000000003', '22000000-0000-0000-0000-000000000003', 'DIRECTOR', NULL);

INSERT INTO trailers (id, movie_id, title, video_url, thumbnail_url, is_primary) VALUES
    (
        '23000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        'Official Trailer',
        'https://example.com/trailers/avatar-the-way-of-water.mp4',
        'https://example.com/trailers/avatar-the-way-of-water.jpg',
        TRUE
    ),
    (
        '23000000-0000-0000-0000-000000000002',
        '20000000-0000-0000-0000-000000000002',
        'Official Trailer',
        'https://example.com/trailers/dune-part-two.mp4',
        'https://example.com/trailers/dune-part-two.jpg',
        TRUE
    );

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
