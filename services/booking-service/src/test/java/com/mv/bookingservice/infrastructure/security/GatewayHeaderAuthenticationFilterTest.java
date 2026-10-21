package com.mv.bookingservice.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayHeaderAuthenticationFilterTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** Runs the filter and returns the Authentication seen by the next filter / controller. */
    private static Authentication run(MockHttpServletRequest request) throws Exception {
        AtomicReference<Authentication> seen = new AtomicReference<>();
        new GatewayHeaderAuthenticationFilter().doFilter(request, new MockHttpServletResponse(),
                (req, res) -> seen.set(SecurityContextHolder.getContext().getAuthentication()));
        return seen.get();
    }

    @Test
    void gatewayHeadersBecomeTheSecurityContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "10000000-0000-0000-0000-000000000004");
        request.addHeader("X-User-Role", "ADMIN");

        Authentication auth = run(request);

        assertEquals(UUID.fromString("10000000-0000-0000-0000-000000000004"), auth.getPrincipal());
        assertTrue(auth.isAuthenticated());
        assertEquals("ROLE_ADMIN", auth.getAuthorities().iterator().next().getAuthority());
        // CurrentUser reads the same context
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertTrue(CurrentUser.hasRole("ADMIN"));
    }

    @Test
    void unknownRoleFallsBackToCustomer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", UUID.randomUUID().toString());
        request.addHeader("X-User-Role", "SUPERUSER");

        assertEquals("ROLE_CUSTOMER", run(request).getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void noOrInvalidUserIdMeansAnonymous() throws Exception {
        assertNull(run(new MockHttpServletRequest()));

        MockHttpServletRequest bad = new MockHttpServletRequest();
        bad.addHeader("X-User-Id", "not-a-uuid");
        bad.addHeader("X-User-Role", "ADMIN");
        assertNull(run(bad));
    }
}
