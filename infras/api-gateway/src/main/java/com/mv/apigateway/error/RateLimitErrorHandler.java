package com.mv.apigateway.error;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
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
import java.time.OffsetDateTime;

@Component
public class RateLimitErrorHandler implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(response) {
            private boolean rateLimited;
            private boolean bodyWritten;

            @Override
            public boolean setStatusCode(HttpStatusCode status) {
                rateLimited = HttpStatus.TOO_MANY_REQUESTS.equals(status);
                return super.setStatusCode(status);
            }

            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                bodyWritten = true;
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> setComplete() {
                if (rateLimited && !bodyWritten) {
                    return writeRateLimitResponse(exchange, this);
                }
                return super.setComplete();
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Mono<Void> writeRateLimitResponse(ServerWebExchange exchange, ServerHttpResponse response) {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HttpHeaders.RETRY_AFTER, "1");

        String body = """
                {"status":429,"code":"RATE_LIMIT_EXCEEDED","message":"Too many requests. Please try again later.","path":"%s","timestamp":"%s"}
                """.formatted(exchange.getRequest().getPath().value(), OffsetDateTime.now());

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
