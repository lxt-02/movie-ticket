package com.mv.searchservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Movie search service.
 * <pre>
 * movie-service --(outbox)--> Kafka topic movie-events --> search-service --> Elasticsearch index "movies"
 *                                                                       <-- GET /api/v1/search/movies
 * </pre>
 * MySQL in movie-service stays the source of truth; the index is a read model that can be rebuilt any time
 * (POST /api/v1/movies/search-sync re-sends every movie).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
