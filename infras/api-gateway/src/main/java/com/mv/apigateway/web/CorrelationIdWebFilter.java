package com.mv.apigateway.web;

import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Starts the correlation ID of a user action: keeps a valid {@code X-Correlation-Id} sent by the client, otherwise
 * creates one. It is forwarded to the service (which logs it and passes it on with Feign) and returned in the
 * response, so the frontend can show it in an error message. Runs before Spring Security (order -100), so 401 / 403
 * answers carry it too.
 */
@Component
@Order(-200)
public class CorrelationIdWebFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-Id";
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String id = incoming != null && VALID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest().mutate().headers(h -> h.set(HEADER, id)).build();
        exchange.getResponse().beforeCommit(() -> {
            // The service answers with the same ID; keep exactly one value.
            exchange.getResponse().getHeaders().set(HEADER, id);
            return Mono.empty();
        });
        return chain.filter(exchange.mutate().request(request).build());
    }
}
