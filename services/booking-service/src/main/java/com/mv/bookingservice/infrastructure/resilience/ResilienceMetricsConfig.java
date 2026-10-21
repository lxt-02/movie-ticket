package com.mv.bookingservice.infrastructure.resilience;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

/**
 * Publishes the Resilience4j state as Micrometer metrics (GET /actuator/prometheus), e.g.
 * {@code resilience4j_circuitbreaker_state{name="showtime",state="open"} 1}, shown on the Grafana dashboard.
 */
@Configuration
public class ResilienceMetricsConfig {

    public ResilienceMetricsConfig(MeterRegistry meterRegistry, CircuitBreakerRegistry breakers,
                                   RetryRegistry retries, BulkheadRegistry bulkheads) {
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(breakers).bindTo(meterRegistry);
        TaggedRetryMetrics.ofRetryRegistry(retries).bindTo(meterRegistry);
        TaggedBulkheadMetrics.ofBulkheadRegistry(bulkheads).bindTo(meterRegistry);
    }
}
