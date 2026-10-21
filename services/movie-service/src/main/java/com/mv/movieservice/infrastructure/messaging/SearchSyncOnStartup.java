package com.mv.movieservice.infrastructure.messaging;

import com.mv.movieservice.application.service.MovieSearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * On startup, queues every movie for search-service (movie.search.sync-on-startup, default true).
 * A fresh Elasticsearch index is filled without any manual step, and the seed movies inserted by Flyway are
 * indexed too. Idempotent on the search side, so restarts and several instances are harmless.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "movie.search", name = "sync-on-startup", havingValue = "true", matchIfMissing = true)
public class SearchSyncOnStartup implements ApplicationRunner {

    private final MovieSearchSyncService movieSearchSyncService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            movieSearchSyncService.resyncAll();
        } catch (RuntimeException e) {
            // Never block the startup for search; an admin can resync later (POST /api/v1/movies/search-sync).
            log.warn("Search resync on startup failed: {}", e.getMessage());
        }
    }
}
