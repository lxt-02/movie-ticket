package com.mv.searchservice.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Creates the movie index at startup. If Elasticsearch is not up yet the service still starts: the index is
 * created on the first write or search instead (MovieIndexClient.ensureIndex).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "search.elasticsearch", name = "init-on-startup", havingValue = "true", matchIfMissing = true)
public class IndexInitializer {

    private final MovieIndexClient movieIndexClient;

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexIfMissing() {
        try {
            movieIndexClient.ensureIndex();
        } catch (RuntimeException e) {
            log.warn("Elasticsearch not ready at startup ({}); the index will be created on first use", e.getMessage());
        }
    }
}
