package com.mv.bookingservice.infrastructure.client;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FeignHeaderPropagationConfigTest {

    static final UUID USER = UUID.fromString("10000000-0000-0000-0000-000000000001");

    /** What GatewayHeaderAuthenticationFilter puts in the context for a request coming from the gateway. */
    static void loggedInCustomer() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                USER, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Email", "customer.demo%40example.com");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put("correlationId", "cid-123");
    }

    @AfterEach
    void clear() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void passesTheCallerFromTheSecurityContextAndTheCorrelationId() {
        loggedInCustomer();

        RequestTemplate template = new RequestTemplate();
        FeignHeaderPropagationConfig.propagate(template);

        assertEquals(List.of(USER.toString()), List.copyOf(template.headers().get("X-User-Id")));
        assertEquals(List.of("CUSTOMER"), List.copyOf(template.headers().get("X-User-Role")));
        assertEquals(List.of("customer.demo%40example.com"), List.copyOf(template.headers().get("X-User-Email")));
        assertEquals(List.of("cid-123"), List.copyOf(template.headers().get("X-Correlation-Id")));
    }

    @Test
    void outsideARequestSendsNoUserButANewCorrelationId() {
        RequestTemplate template = new RequestTemplate();
        FeignHeaderPropagationConfig.propagate(template);

        assertFalse(template.headers().containsKey("X-User-Id"));
        assertFalse(template.headers().containsKey("X-User-Role"));
        Collection<String> id = template.headers().get("X-Correlation-Id");
        assertNotNull(id);
        assertEquals(1, id.size());
    }
}
