package com.mv.apigateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component("apiKeyResolver")
public class ApiKeyResolver implements KeyResolver {

    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String DEFAULT_SCOPE = "integration-api";

    private final RateLimitKeyFactory keyFactory;

    public ApiKeyResolver(RateLimitKeyFactory keyFactory) {
        this.keyFactory = keyFactory;
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        if (!StringUtils.hasText(apiKey)) {
            return Mono.empty();
        }
        String fingerprint = DigestUtils.md5DigestAsHex(apiKey.getBytes(StandardCharsets.UTF_8));
        return Mono.just(keyFactory.apiKey(fingerprint, DEFAULT_SCOPE));
    }
}
