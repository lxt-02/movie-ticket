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
