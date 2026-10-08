package com.mv.apigateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component("ipKeyResolver")
public class IpKeyResolver implements KeyResolver {

    private static final String DEFAULT_SCOPE = "general-api";

    private final RateLimitKeyFactory keyFactory;

    public IpKeyResolver(RateLimitKeyFactory keyFactory) {
        this.keyFactory = keyFactory;
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return Mono.empty();
        }
        return Mono.just(keyFactory.ipKey(remoteAddress.getAddress().getHostAddress(), DEFAULT_SCOPE));
    }
}
