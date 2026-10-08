package com.mv.apigateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Component("userKeyResolver")
@Primary
public class UserKeyResolver implements KeyResolver {

    private static final String DEFAULT_SCOPE = "general-api";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final IpKeyResolver ipKeyResolver;
    private final RateLimitKeyFactory keyFactory;

    public UserKeyResolver(IpKeyResolver ipKeyResolver, RateLimitKeyFactory keyFactory) {
        this.ipKeyResolver = ipKeyResolver;
        this.keyFactory = keyFactory;
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .map(Principal::getName)
                .filter(StringUtils::hasText)
                .map(userId -> keyFactory.userKey(userId, DEFAULT_SCOPE))
                .switchIfEmpty(resolveDevelopmentUser(exchange))
                .switchIfEmpty(ipKeyResolver.resolve(exchange));
    }

    private Mono<String> resolveDevelopmentUser(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
        if (!StringUtils.hasText(userId)) {
            return Mono.empty();
        }
        return Mono.just(keyFactory.userKey(userId, DEFAULT_SCOPE));
    }
}
