package com.mv.apigateway.api.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/internal/gateway")
public class PingController {

    @GetMapping("/ping")
    public Mono<Map<String, Object>> ping() {
        return Mono.just(Map.of(
                "success", true,
                "message", "Gateway ping is reachable",
                "data", Map.of("status", "pong"),
                "timestamp", Instant.now()
        ));
    }
}
