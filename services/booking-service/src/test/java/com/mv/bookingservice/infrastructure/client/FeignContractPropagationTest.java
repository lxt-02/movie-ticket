package com.mv.bookingservice.infrastructure.client;

import com.sun.net.httpserver.HttpServer;
import feign.Feign;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.web.context.request.RequestContextHolder;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Real HTTP call through booking's own ShowtimeFeignClient (mappings declared in infrastructure.client),
 * with the header propagation interceptor: checks what showtime-service would receive.
 */
class FeignContractPropagationTest {

    private HttpServer server;
    private final Map<String, String> received = new ConcurrentHashMap<>();
    private ShowtimeFeignClient client;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            received.put("method", exchange.getRequestMethod());
            received.put("path", exchange.getRequestURI().getPath());
            String user = exchange.getRequestHeaders().getFirst("X-User-Id");
            if (user != null) {
                received.put("userId", user);
            }
            String role = exchange.getRequestHeaders().getFirst("X-User-Role");
            if (role != null) {
                received.put("role", role);
            }
            String cid = exchange.getRequestHeaders().getFirst("X-Correlation-Id");
            if (cid != null) {
                received.put("correlationId", cid);
            }
            byte[] body = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        client = Feign.builder()
                .contract(new SpringMvcContract())
                .requestInterceptor(new FeignHeaderPropagationConfig().headerPropagationInterceptor())
                .decoder((response, type) -> null) // only the request matters here
                .target(ShowtimeFeignClient.class, "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stop() {
        server.stop(0);
        MDC.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void contractPathAndUserHeadersReachTheOtherService() {
        FeignHeaderPropagationConfigTest.loggedInCustomer();
        MDC.put("correlationId", "demo-001");
        UUID holdId = UUID.randomUUID();

        client.releaseHold(holdId);

        assertEquals("POST", received.get("method"));
        assertEquals("/internal/holds/" + holdId + "/release", received.get("path"));
        assertEquals(FeignHeaderPropagationConfigTest.USER.toString(), received.get("userId"));
        assertEquals("CUSTOMER", received.get("role"));
        assertEquals("demo-001", received.get("correlationId"));
    }

    @Test
    void backgroundCallHasNoUserButACorrelationId() {
        client.confirmHold(UUID.randomUUID());

        assertNull(received.get("userId"));
        assertNotNull(received.get("correlationId"));
    }
}
