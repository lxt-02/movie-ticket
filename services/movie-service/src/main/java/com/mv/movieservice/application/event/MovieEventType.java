package com.mv.movieservice.application.event;

/**
 * Types of the messages on topic movie-events. Every message carries the full movie snapshot, so a consumer
 * handles all of them the same way (upsert, or remove when the movie is ARCHIVED); the type says why it was sent.
 */
public enum MovieEventType {
    MOVIE_CREATED,
    MOVIE_UPDATED,
    MOVIE_STATUS_CHANGED,
    MOVIE_ARCHIVED,
    /** Re-sent by a full resync (startup or admin), not by a change. */
    MOVIE_SYNCED
}
