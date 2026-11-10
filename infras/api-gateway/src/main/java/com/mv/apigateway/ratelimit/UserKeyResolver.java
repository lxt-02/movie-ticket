package com.mv.apigateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;

/**
 * Per-user key: {@code rl:user:{userId}:{routeId}}.
 * <p>
 * Identity source, in order:
 * <ol>
 *   <li>Authenticated principal (production, once JWT is integrated).</li>
 *   <li>{@code X-User-Id} header, only when {@code app.rate-limit.trust-user-id-header=true} (dev/demo).</li>
 * </ol>
 * No identity returns an empty key, so the route rejects the request (deny-empty-key -> 401)
 * instead of silently falling back to another bucket.
 */
@Component("userKeyResolver")
@Primary // Spring Cloud Gateway injects one default KeyResolver by type
public class UserKeyResolver implements KeyResolver {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final RateLimitKeyFactory keyFactory;
    private final RateLimitProperties properties;

    public UserKeyResolver(RateLimitKeyFactory keyFactory, RateLimitProperties properties) {
        this.keyFactory = keyFactory;
        this.properties = properties;
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .map(Principal::getName)
                .filter(StringUtils::hasText)
                .switchIfEmpty(Mono.defer(() -> resolveDevelopmentUser(exchange)))
                .map(userId -> keyFactory.userKey(userId, exchange));
    }

    private Mono<String> resolveDevelopmentUser(ServerWebExchange exchange) {
        if (!properties.isTrustUserIdHeader()) {
            return Mono.empty();
        }
        String userId = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
        return StringUtils.hasText(userId) ? Mono.just(userId) : Mono.empty();
    }
}
