package com.mv.paymentservice.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on the outbox relay and the expiration scheduler. Disabled in tests with payment.scheduling.enabled=false.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "payment.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
