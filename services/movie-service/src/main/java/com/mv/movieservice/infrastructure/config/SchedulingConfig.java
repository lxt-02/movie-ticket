package com.mv.movieservice.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on the movie outbox relay. Disabled in tests with movie.scheduling.enabled=false. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "movie.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
