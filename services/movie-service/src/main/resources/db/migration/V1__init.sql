CREATE TABLE movies (
    id CHAR(36) PRIMARY KEY,
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
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHECK (status IN ('COMING_SOON', 'NOW_SHOWING', 'ENDED', 'ARCHIVED'))
);

CREATE TABLE genres (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE movie_genres (
    movie_id CHAR(36) NOT NULL,
    genre_id CHAR(36) NOT NULL,
    PRIMARY KEY (movie_id, genre_id),
    CONSTRAINT fk_movie_genres_movie FOREIGN KEY (movie_id) REFERENCES movies(id),
    CONSTRAINT fk_movie_genres_genre FOREIGN KEY (genre_id) REFERENCES genres(id)
);

CREATE TABLE persons (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    avatar_url TEXT,
    biography TEXT
);

CREATE TABLE movie_crew (
    movie_id CHAR(36) NOT NULL,
    person_id CHAR(36) NOT NULL,
    role VARCHAR(30) NOT NULL,
    character_name VARCHAR(150),
    PRIMARY KEY (movie_id, person_id, role),
    CONSTRAINT fk_movie_crew_movie FOREIGN KEY (movie_id) REFERENCES movies(id),
    CONSTRAINT fk_movie_crew_person FOREIGN KEY (person_id) REFERENCES persons(id),
    CHECK (role IN ('DIRECTOR', 'ACTOR'))
);

CREATE TABLE trailers (
    id CHAR(36) PRIMARY KEY,
    movie_id CHAR(36) NOT NULL,
    title VARCHAR(255),
    video_url TEXT NOT NULL,
    thumbnail_url TEXT,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_trailers_movie FOREIGN KEY (movie_id) REFERENCES movies(id)
);

CREATE TABLE outbox_events (
    id CHAR(36) PRIMARY KEY,
    aggregate_id CHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    published_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
