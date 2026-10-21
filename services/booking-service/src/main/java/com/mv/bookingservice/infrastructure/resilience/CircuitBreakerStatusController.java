package com.mv.bookingservice.infrastructure.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only view of the resilience layers, for monitoring and the demo (not routed by the gateway):
 * <ul>
 *     <li>GET http://localhost:8083/internal/resilience: everything below in one answer</li>
 *     <li>GET /internal/resilience/circuit-breakers</li>
 *     <li>GET /internal/resilience/retries</li>
 *     <li>GET /internal/resilience/bulkheads</li>
 * </ul>
 */
@RestController
@RequestMapping("/internal/resilience")
@RequiredArgsConstructor
public class CircuitBreakerStatusController {

    private final CircuitBreakerRegistry breakers;
    private final RetryRegistry retries;
    private final BulkheadRegistry bulkheads;

    @GetMapping
    public Map<String, Object> all() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("circuitBreakers", circuitBreakers());
        view.put("retries", retries());
        view.put("bulkheads", bulkheads());
        return view;
    }

    @GetMapping("/circuit-breakers")
    public List<Map<String, Object>> circuitBreakers() {
        return breakers.getAllCircuitBreakers().stream()
                .sorted(Comparator.comparing(CircuitBreaker::getName))
                .map(this::describe)
                .toList();
    }

    @GetMapping("/retries")
    public List<Map<String, Object>> retries() {
        return retries.getAllRetries().stream()
                .sorted(Comparator.comparing(Retry::getName))
                .map(retry -> {
                    Retry.Metrics m = retry.getMetrics();
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("name", retry.getName());
                    view.put("maxAttempts", retry.getRetryConfig().getMaxAttempts());
                    view.put("successWithoutRetry", m.getNumberOfSuccessfulCallsWithoutRetryAttempt());
                    view.put("successAfterRetry", m.getNumberOfSuccessfulCallsWithRetryAttempt());
                    view.put("failedAfterRetry", m.getNumberOfFailedCallsWithRetryAttempt());
                    view.put("failedWithoutRetry", m.getNumberOfFailedCallsWithoutRetryAttempt());
                    return view;
                })
                .toList();
    }

    @GetMapping("/bulkheads")
    public List<Map<String, Object>> bulkheads() {
        return bulkheads.getAllBulkheads().stream()
                .sorted(Comparator.comparing(Bulkhead::getName))
                .map(bulkhead -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("name", bulkhead.getName());
                    view.put("maxConcurrentCalls", bulkhead.getBulkheadConfig().getMaxConcurrentCalls());
                    view.put("availableSlots", bulkhead.getMetrics().getAvailableConcurrentCalls());
                    view.put("maxWaitMillis", bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis());
                    return view;
                })
                .toList();
    }

    private Map<String, Object> describe(CircuitBreaker breaker) {
        CircuitBreaker.Metrics m = breaker.getMetrics();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("name", breaker.getName());
        view.put("state", breaker.getState().name());
        view.put("failureRatePercent", m.getFailureRate()); // -1 until minimumNumberOfCalls is reached
        view.put("bufferedCalls", m.getNumberOfBufferedCalls());
        view.put("failedCalls", m.getNumberOfFailedCalls());
        view.put("successfulCalls", m.getNumberOfSuccessfulCalls());
        view.put("notPermittedCalls", m.getNumberOfNotPermittedCalls());
        view.put("failureRateThreshold", breaker.getCircuitBreakerConfig().getFailureRateThreshold());
        view.put("minimumNumberOfCalls", breaker.getCircuitBreakerConfig().getMinimumNumberOfCalls());
        return view;
    }
}
