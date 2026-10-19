package com.mv.apigateway.api.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demo target for the rate-limit routes (/rate-limit/{user|ip|api-key}/ping).
 * Echoes the caller identity so the demo shows which bucket was used.
 */
@RestController
@RequestMapping("/internal/gateway")
public class PingController {

    @GetMapping("/ping")
    public Mono<Map<String, Object>> ping(ServerWebExchange exchange) {
        Map<String, Object> caller = new LinkedHashMap<>();
        caller.put("userId", exchange.getRequest().getHeaders().getFirst("X-User-Id"));
        caller.put("apiKey", mask(exchange.getRequest().getHeaders().getFirst("X-Api-Key")));
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        caller.put("clientIp", remote != null && remote.getAddress() != null
                ? remote.getAddress().getHostAddress() : null);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "Gateway ping is reachable");
        body.put("data", Map.of("status", "pong", "caller", caller));
        body.put("timestamp", Instant.now());
        return Mono.just(body);
    }

    private static String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        String trimmed = apiKey.trim();
        return trimmed.length() <= 4 ? "****" : trimmed.substring(0, 4) + "****";
    }
}
