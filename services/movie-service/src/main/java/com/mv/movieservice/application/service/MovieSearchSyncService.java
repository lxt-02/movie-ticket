package com.mv.movieservice.application.service;

import com.mv.movieservice.application.dto.PageResult;
import com.mv.movieservice.application.event.MovieEventType;
import com.mv.movieservice.application.port.out.LoadMoviePort;
import com.mv.movieservice.application.port.out.MovieEventPort;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Re-sends every movie (archived ones included, so search-service removes them) as MOVIE_SYNCED events.
 * Used to fill an empty / recreated Elasticsearch index and to repair drift. Safe to run any time: search-service
 * upserts by movie id with the movie's updatedAt as version, so a resync never overwrites a newer change.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieSearchSyncService {

    static final int PAGE_SIZE = 200;

    private final LoadMoviePort loadMoviePort;
    private final MovieEventPort movieEventPort;

    /** @return number of movies queued for publication */
    @Transactional
    public int resyncAll() {
        int page = 0;
        int count = 0;
        while (true) {
            PageResult<Movie> result = loadMoviePort.search(null, null, true, page, PAGE_SIZE);
            for (Movie movie : result.items()) {
                movieEventPort.publish(movie, MovieEventType.MOVIE_SYNCED);
                count++;
            }
            if (result.items().size() < PAGE_SIZE) {
                break;
            }
            page++;
        }
        log.info("Search resync: {} movie event(s) written to the outbox", count);
        return count;
    }
}
