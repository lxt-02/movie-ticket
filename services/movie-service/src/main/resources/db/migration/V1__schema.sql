-- movie-service schema (PostgreSQL). Every id is a native UUID; times are TIMESTAMPTZ (java.time.Instant).

CREATE TABLE movies (
    id               UUID         PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    original_title   VARCHAR(255),
    description      TEXT,
    duration_minutes INT          NOT NULL,
    release_date     DATE,
    age_rating       VARCHAR(20),
    language         VARCHAR(50),
    country          VARCHAR(100),
    poster_url       TEXT,
    status           VARCHAR(30)  NOT NULL DEFAULT 'COMING_SOON',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_movies_duration CHECK (duration_minutes > 0),
    CONSTRAINT ck_movies_status CHECK (status IN ('COMING_SOON', 'NOW_SHOWING', 'ENDED', 'ARCHIVED')),
    -- Vietnamese film classification: P, K, T13, T16, T18 (or none).
    CONSTRAINT ck_movies_age_rating CHECK (age_rating IS NULL OR age_rating IN ('P', 'K', 'T13', 'T16', 'T18'))
);

CREATE TABLE genres (
    id   UUID         PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE movie_genres (
    movie_id UUID NOT NULL,
    genre_id UUID NOT NULL,
    PRIMARY KEY (movie_id, genre_id),
    CONSTRAINT fk_movie_genres_movie FOREIGN KEY (movie_id) REFERENCES movies (id),
    CONSTRAINT fk_movie_genres_genre FOREIGN KEY (genre_id) REFERENCES genres (id)
);

CREATE TABLE persons (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    avatar_url TEXT,
    biography  TEXT
);

CREATE TABLE movie_crew (
    movie_id       UUID         NOT NULL,
    person_id      UUID         NOT NULL,
    role           VARCHAR(30)  NOT NULL,
    character_name VARCHAR(150),
    PRIMARY KEY (movie_id, person_id, role),
    CONSTRAINT fk_movie_crew_movie FOREIGN KEY (movie_id) REFERENCES movies (id),
    CONSTRAINT fk_movie_crew_person FOREIGN KEY (person_id) REFERENCES persons (id),
    CONSTRAINT ck_movie_crew_role CHECK (role IN ('DIRECTOR', 'ACTOR'))
);

CREATE TABLE trailers (
    id            UUID         PRIMARY KEY,
    movie_id      UUID         NOT NULL,
    title         VARCHAR(255),
    video_url     TEXT         NOT NULL,
    thumbnail_url TEXT,
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_trailers_movie FOREIGN KEY (movie_id) REFERENCES movies (id)
);

CREATE INDEX idx_movie_genres_genre ON movie_genres (genre_id);
CREATE INDEX idx_movie_crew_person ON movie_crew (person_id);
CREATE INDEX idx_trailers_movie ON trailers (movie_id);

-- Transactional outbox: movie events for search-service (topic movie-events).
CREATE TABLE outbox_events (
    id           UUID         PRIMARY KEY,
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB        NOT NULL,
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_movie_outbox_pending ON outbox_events (published_at, created_at);

-- One active outbox relay when several movie-service instances run (row locked with FOR UPDATE SKIP LOCKED).
CREATE TABLE outbox_relay_lock (
    name VARCHAR(50) PRIMARY KEY
);
INSERT INTO outbox_relay_lock (name) VALUES ('movie-outbox');
