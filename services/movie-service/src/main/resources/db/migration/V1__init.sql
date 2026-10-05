CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE movies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    original_title VARCHAR(255),
    description TEXT,
    duration_minutes INT NOT NULL CHECK (duration_minutes > 0),
    release_date DATE,
    age_rating VARCHAR(20),
    language VARCHAR(50),
    country VARCHAR(100),
    poster_url TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'COMING_SOON',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (status IN ('COMING_SOON', 'NOW_SHOWING', 'ENDED', 'ARCHIVED'))
);

CREATE TABLE genres (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE movie_genres (
    movie_id UUID NOT NULL REFERENCES movies(id),
    genre_id UUID NOT NULL REFERENCES genres(id),
    PRIMARY KEY (movie_id, genre_id)
);

CREATE TABLE persons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    avatar_url TEXT,
    biography TEXT
);

CREATE TABLE movie_crew (
    movie_id UUID NOT NULL REFERENCES movies(id),
    person_id UUID NOT NULL REFERENCES persons(id),
    role VARCHAR(30) NOT NULL,
    character_name VARCHAR(150),
    PRIMARY KEY (movie_id, person_id, role),
    CHECK (role IN ('DIRECTOR', 'ACTOR'))
);

CREATE TABLE trailers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL REFERENCES movies(id),
    title VARCHAR(255),
    video_url TEXT NOT NULL,
    thumbnail_url TEXT,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
