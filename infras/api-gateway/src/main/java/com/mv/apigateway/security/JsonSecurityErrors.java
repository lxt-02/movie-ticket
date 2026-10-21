package com.mv.apigateway.security;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** 401 / 403 written by the gateway, as the usual JSON error body in Vietnamese. */
final class JsonSecurityErrors {

    private JsonSecurityErrors() {
    }

    static Mono<Void> unauthorized(ServerWebExchange exchange, AuthenticationException ex) {
        exchange.getResponse().getHeaders().set("WWW-Authenticate", "Bearer");
        return write(exchange, HttpStatus.UNAUTHORIZED,
                "Bạn cần đăng nhập (token không có, không hợp lệ hoặc đã hết hạn)");
    }

    static Mono<Void> forbidden(ServerWebExchange exchange, AccessDeniedException ex) {
        return write(exchange, HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
    }

    private static Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        String path = exchange.getRequest().getPath().value().replace("\\", "\\\\").replace("\"", "\\\"");
        String body = "{\"success\":false,\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase() + "\",\"message\":\"" + message
                + "\",\"path\":\"" + path + "\",\"timestamp\":\"" + Instant.now() + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
