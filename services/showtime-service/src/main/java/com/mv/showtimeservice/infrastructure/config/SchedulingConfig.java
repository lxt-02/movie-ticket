package com.mv.showtimeservice.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Showtime lifecycle job. Disabled in tests (showtime.scheduling.enabled=false). */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "showtime.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
