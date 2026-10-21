package com.mv.showtimeservice.infrastructure.client;

import com.mv.showtimeservice.infrastructure.security.CurrentUser;
import com.mv.showtimeservice.infrastructure.web.CorrelationIdFilter;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

/**
 * Header propagation for every Feign call of this service ({@link RequestInterceptor}, applied to all clients).
 * The gateway sent the caller as headers; they are passed on, so the next service builds the same
 * SecurityContext:
 * <ul>
 *     <li>{@code X-User-Id}, {@code X-User-Role}: from the {@code SecurityContextHolder} of the current request.</li>
 *     <li>{@code X-User-Email}, {@code X-User-Name}: copied from the current request (used by user-service).</li>
 *     <li>{@code X-Correlation-Id}: from the MDC, so the logs of all services for one user action share one ID.</li>
 * </ul>
 * Calls made outside an HTTP request (Kafka listener, scheduled job) have no user: only a new correlation ID is sent.
 */
@Configuration
public class FeignHeaderPropagationConfig {

    static final List<String> COPIED_HEADERS = List.of("X-User-Email", "X-User-Name");

    @Bean
    public RequestInterceptor headerPropagationInterceptor() {
        return FeignHeaderPropagationConfig::propagate;
    }

    static void propagate(RequestTemplate template) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        template.header(CorrelationIdFilter.HEADER,
                correlationId != null ? correlationId : UUID.randomUUID().toString());

        CurrentUser.id().ifPresent(id -> template.header("X-User-Id", id.toString()));
        CurrentUser.role().ifPresent(role -> template.header("X-User-Role", role));

        HttpServletRequest request = currentRequest();
        if (request != null) {
            for (String name : COPIED_HEADERS) {
                String value = request.getHeader(name);
                if (value != null && !value.isBlank()) {
                    template.header(name, value);
                }
            }
        }
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }
}
