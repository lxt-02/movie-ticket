package com.mv.movieservice.application.port.out;

import com.mv.movieservice.application.event.MovieEventType;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;

/**
 * Announces a movie change to other services (search-service indexes it in Elasticsearch).
 * Must be called inside the transaction that saved the movie: the event is stored in the outbox with it.
 */
public interface MovieEventPort {

    void publish(Movie movie, MovieEventType type);
}
