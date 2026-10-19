package com.mv.apigateway.error;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.OptionalLong;

/**
 * Adds a JSON body to responses that RequestRateLimiter completes without a body:
 * <ul>
 *   <li>429 Too Many Requests: bucket has no tokens (+ Retry-After).</li>
 *   <li>401 Unauthorized: no resolvable identity (deny-empty-key).</li>
 * </ul>
 * Proxied downstream responses always write a body, so they are never rewritten.
 * Body format matches the services' ErrorResponse (success, status, error, message, path, timestamp) plus a code.
 */
@Component
public class RateLimitErrorHandler implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitErrorHandler.class);

    private static final String REPLENISH_RATE_HEADER = "X-RateLimit-Replenish-Rate";
    private static final String REQUESTED_TOKENS_HEADER = "X-RateLimit-Requested-Tokens";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
            private boolean bodyWritten;

            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                bodyWritten = true;
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                bodyWritten = true;
                return super.writeAndFlushWith(body);
            }

            @Override
            public Mono<Void> setComplete() {
                HttpStatusCode status = getStatusCode();
                if (!bodyWritten && !isCommitted() && status != null) {
                    if (status.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                        return writeRateLimitExceeded(exchange, this);
                    }
                    if (status.value() == HttpStatus.UNAUTHORIZED.value()) {
                        return writeMissingIdentity(exchange, this);
                    }
                }
                return super.setComplete();
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    @Override
    public int getOrder() {
        // Wrap the response before RequestRateLimiter runs.
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Mono<Void> writeRateLimitExceeded(ServerWebExchange exchange, ServerHttpResponse response) {
        HttpHeaders headers = response.getHeaders();
        retryAfterSeconds(headers).ifPresent(seconds -> headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(seconds)));

        log.warn("Rate limit exceeded: route={}, method={}, path={}",
                routeId(exchange), exchange.getRequest().getMethod(), exchange.getRequest().getPath().value());

        return writeJson(exchange, response, HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMIT_EXCEEDED", "Too many requests. Please try again later.");
    }

    private Mono<Void> writeMissingIdentity(ServerWebExchange exchange, ServerHttpResponse response) {
        log.warn("Rate limit identity missing: route={}, path={}",
                routeId(exchange), exchange.getRequest().getPath().value());

        return writeJson(exchange, response, HttpStatus.UNAUTHORIZED,
                "RATE_LIMIT_IDENTITY_MISSING", "Unable to identify the caller for rate limiting.");
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, ServerHttpResponse response,
                                 HttpStatus status, String code, String message) {
        String body = "{"
                + "\"success\":false,"
                + "\"status\":" + status.value() + ","
                + "\"error\":\"" + escape(status.getReasonPhrase()) + "\","
                + "\"code\":\"" + escape(code) + "\","
                + "\"message\":\"" + escape(message) + "\","
                + "\"path\":\"" + escape(exchange.getRequest().getPath().value()) + "\","
                + "\"timestamp\":\"" + Instant.now() + "\""
                + "}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().setContentLength(bytes.length);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Approximate wait for the requested tokens to refill: ceil(requestedTokens / replenishRate).
     * Values come from the headers RedisRateLimiter adds; Retry-After is omitted if they are missing.
     */
    private OptionalLong retryAfterSeconds(HttpHeaders headers) {
        try {
            double replenishRate = Double.parseDouble(headers.getFirst(REPLENISH_RATE_HEADER));
            double requestedTokens = Double.parseDouble(headers.getFirst(REQUESTED_TOKENS_HEADER));
            if (replenishRate <= 0) {
                return OptionalLong.empty();
            }
            return OptionalLong.of((long) Math.max(1, Math.ceil(requestedTokens / replenishRate)));
        } catch (RuntimeException e) {
            return OptionalLong.empty();
        }
    }

    private String routeId(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route != null ? route.getId() : "unknown";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
