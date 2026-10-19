package com.mv.bookingservice.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on the booking outbox relay. Disabled in tests with booking.scheduling.enabled=false.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "booking.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
