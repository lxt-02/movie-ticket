package com.mv.apigateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Per-API-key key: {@code rl:apikey:{sha256-fingerprint}:{routeId}}.
 * The raw key is never stored in Redis, only a SHA-256 fingerprint.
 * <p>
 * Known limitation (demo): the key is not validated here. In production, validate the key
 * (or resolve it to a partner ID) before the limiter runs, otherwise random keys get new buckets.
 */
@Component("apiKeyResolver")
public class ApiKeyResolver implements KeyResolver {

    private static final String API_KEY_HEADER = "X-Api-Key";

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
        return Mono.just(keyFactory.apiKey(fingerprint(apiKey.trim()), exchange));
    }

    private static String fingerprint(String apiKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
