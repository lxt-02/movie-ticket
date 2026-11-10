package com.mv.apigateway.ratelimit;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * Builds rate-limit keys in the form {@code rl:{identity-type}:{identity}:{scope}}.
 * The scope is the matched route ID, so each route has its own bucket per caller.
 */
@Component
public class RateLimitKeyFactory {

    private static final String DEFAULT_SCOPE = "general-api";

    public String userKey(String userId, ServerWebExchange exchange) {
        return key("user", userId, scope(exchange));
    }

    public String ipKey(String clientIp, ServerWebExchange exchange) {
        return key("ip", clientIp, scope(exchange));
    }

    public String apiKey(String keyFingerprint, ServerWebExchange exchange) {
        return key("apikey", keyFingerprint, scope(exchange));
    }

    private String scope(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route != null ? route.getId() : DEFAULT_SCOPE;
    }

    private String key(String identityType, String identity, String scope) {
        return "rl:%s:%s:%s".formatted(identityType, normalize(identity), normalize(scope));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9._:-]", "_");
    }
}
