package com.mv.apigateway.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

/**
 * Errors raised by the gateway itself (no route, service not registered / down, timeout) as the usual JSON
 * error body with a Vietnamese message. Responses proxied from services are not touched.
 */
@Component
@Order(-2) // before Spring Boot's default handler (-1)
public class GatewayErrorHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        HttpStatusCode status;
        String message;
        if (hasCause(ex, TimeoutException.class)) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "Dịch vụ phản hồi quá lâu, vui lòng thử lại sau";
        } else if (hasCause(ex, ConnectException.class)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Dịch vụ tạm thời không khả dụng, vui lòng thử lại sau";
        } else if (ex instanceof ResponseStatusException rse) {
            status = rse.getStatusCode();
            message = switch (status.value()) {
                case 404 -> "Không tìm thấy đường dẫn " + exchange.getRequest().getPath().value();
                case 405 -> "Phương thức không được hỗ trợ cho đường dẫn này";
                case 413 -> "Dữ liệu gửi lên quá lớn";
                case 503 -> "Dịch vụ tạm thời không khả dụng, vui lòng thử lại sau";
                case 504 -> "Dịch vụ phản hồi quá lâu, vui lòng thử lại sau";
                default -> status.is4xxClientError() ? "Yêu cầu không hợp lệ" : "Hệ thống đang gặp sự cố, vui lòng thử lại sau";
            };
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Hệ thống đang gặp sự cố, vui lòng thử lại sau";
        }
        if (status.is5xxServerError()) {
            log.warn("Gateway error {} on {}: {}", status.value(), exchange.getRequest().getPath().value(), ex.toString());
        }
        response.setStatusCode(status);
        response.getHeaders().setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        String reason = status instanceof HttpStatus http ? http.getReasonPhrase() : String.valueOf(status.value());
        String path = exchange.getRequest().getPath().value().replace("\\", "\\\\").replace("\"", "\\\"");
        String body = "{\"success\":false,\"status\":" + status.value() + ",\"error\":\"" + reason
                + "\",\"message\":\"" + message.replace("\"", "'") + "\",\"path\":\"" + path
                + "\",\"timestamp\":\"" + Instant.now() + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private static boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }
}
