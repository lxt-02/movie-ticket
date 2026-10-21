package com.mv.apigateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Turns the validated Keycloak token into the headers the services read:
 * <ul>
 *     <li>{@code X-User-Id}: token "sub" (Keycloak user id, also the id in user-service).</li>
 *     <li>{@code X-User-Role}: highest realm role, ADMIN &gt; STAFF &gt; CUSTOMER.</li>
 *     <li>{@code X-User-Email}, {@code X-User-Name} (URL-encoded): used by user-service to create the profile
 *     on first login.</li>
 * </ul>
 * Copies sent by the client are always removed first, so nobody can pretend to be another user or an admin.
 * Rate-limit demo routes (forwarded to the gateway itself) are left untouched.
 */
@Component
public class UserHeaderRelayFilter implements GlobalFilter, Ordered {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String USER_NAME = "X-User-Name";

    private static final List<String> ROLE_PRIORITY = List.of("ADMIN", "STAFF", "CUSTOMER");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getPath().value().startsWith("/rate-limit/")) {
            return chain.filter(exchange);
        }
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(auth -> ((JwtAuthenticationToken) auth).getToken())
                .map(jwt -> withUser(exchange, jwt))
                .defaultIfEmpty(withUser(exchange, null))
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static ServerWebExchange withUser(ServerWebExchange exchange, Jwt jwt) {
        ServerHttpRequest request = exchange.getRequest().mutate().headers(h -> {
            h.remove(USER_ID);
            h.remove(USER_ROLE);
            h.remove(USER_EMAIL);
            h.remove(USER_NAME);
            if (jwt != null) {
                h.set(USER_ID, jwt.getSubject());
                h.set(USER_ROLE, role(jwt));
                setEncoded(h, USER_EMAIL, jwt.getClaimAsString("email"));
                setEncoded(h, USER_NAME, jwt.getClaimAsString("name"));
            }
        }).build();
        return exchange.mutate().request(request).build();
    }

    private static void setEncoded(org.springframework.http.HttpHeaders headers, String name, String value) {
        if (value != null && !value.isBlank()) {
            headers.set(name, URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }

    /** Keycloak puts realm roles in realm_access.roles. */
    static String role(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles) {
            for (String candidate : ROLE_PRIORITY) {
                if (roles.contains(candidate)) {
                    return candidate;
                }
            }
        }
        return "CUSTOMER";
    }
}
