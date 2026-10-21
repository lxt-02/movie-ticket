package com.mv.movieservice.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The gateway checks the Keycloak token and sends the caller downstream as headers:
 * {@code X-User-Id} (token "sub") and {@code X-User-Role} (ADMIN / STAFF / CUSTOMER). Copies sent by the client
 * are removed by the gateway. This filter turns those headers into an {@code Authentication} in the
 * {@link SecurityContextHolder}: principal = user id, authority = {@code ROLE_<role>}. Spring Security rules
 * ({@code authenticated()}, {@code hasRole}) and {@link CurrentUser} then work as usual.
 * <p>
 * Trust model: the services must only be reachable through the gateway (only the gateway port is published).
 */
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";

    private static final Set<String> ROLES = Set.of("ADMIN", "STAFF", "CUSTOMER");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        UUID userId = parse(request.getHeader(USER_ID));
        if (userId != null) {
            String header = request.getHeader(USER_ROLE);
            String role = header != null && ROLES.contains(header.trim().toUpperCase()) ? header.trim().toUpperCase() : "CUSTOMER";
            UsernamePasswordAuthenticationToken auth = UsernamePasswordAuthenticationToken.authenticated(
                    userId, null, List.of(new SimpleGrantedAuthority(CurrentUser.ROLE_PREFIX + role)));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request, response);
    }

    private static UUID parse(String value) {
        try {
            return value == null || value.isBlank() ? null : UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
