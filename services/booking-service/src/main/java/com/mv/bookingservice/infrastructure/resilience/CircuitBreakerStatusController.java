package com.mv.bookingservice.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only view of the circuit breakers, for monitoring and the demo:
 * GET http://localhost:8083/internal/resilience/circuit-breakers (not routed by the gateway).
 */
@RestController
@RequestMapping("/internal/resilience")
@RequiredArgsConstructor
public class CircuitBreakerStatusController {

    private final CircuitBreakerRegistry registry;

    @GetMapping("/circuit-breakers")
    public List<Map<String, Object>> circuitBreakers() {
        return registry.getAllCircuitBreakers().stream()
                .sorted(Comparator.comparing(CircuitBreaker::getName))
                .map(this::describe)
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
