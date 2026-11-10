package com.mv.apigateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Per-IP key: {@code rl:ip:{clientIp}:{routeId}}.
 * <ul>
 *   <li>{@code app.rate-limit.trusted-proxy-hops=0} (default): Gateway exposed directly,
 *       use the TCP remote address; X-Forwarded-For is ignored, so it cannot be spoofed.</li>
 *   <li>{@code n > 0}: Gateway behind n load balancers / reverse proxies. Read the client IP from
 *       X-Forwarded-For, trusting only the last n entries appended by our proxies. Without this,
 *       every client would share the load balancer's IP and one bucket.</li>
 * </ul>
 */
@Component("ipKeyResolver")
public class IpKeyResolver implements KeyResolver {

    private final RateLimitKeyFactory keyFactory;
    private final RemoteAddressResolver addressResolver;

    public IpKeyResolver(RateLimitKeyFactory keyFactory, RateLimitProperties properties) {
        this.keyFactory = keyFactory;
        int hops = properties.getTrustedProxyHops();
        this.addressResolver = hops > 0
                ? XForwardedRemoteAddressResolver.maxTrustedIndex(hops)
                : new RemoteAddressResolver() {
                };
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = addressResolver.resolve(exchange);
        if (remoteAddress == null) {
            return Mono.empty();
        }
        String clientIp = remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : remoteAddress.getHostString();
        return Mono.just(keyFactory.ipKey(clientIp, exchange));
    }
}
