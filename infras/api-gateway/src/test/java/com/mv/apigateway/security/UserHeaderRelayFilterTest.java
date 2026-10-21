package com.mv.apigateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserHeaderRelayFilterTest {

    private static Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "RS256").subject("10000000-0000-0000-0000-000000000001")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Test
    void picksHighestRealmRole() {
        assertEquals("ADMIN", UserHeaderRelayFilter.role(jwt(Map.of("realm_access",
                Map.of("roles", List.of("default-roles-mss301", "CUSTOMER", "ADMIN"))))));
        assertEquals("STAFF", UserHeaderRelayFilter.role(jwt(Map.of("realm_access",
                Map.of("roles", List.of("STAFF", "CUSTOMER"))))));
    }

    @Test
    void defaultsToCustomer() {
        assertEquals("CUSTOMER", UserHeaderRelayFilter.role(jwt(Map.of("email", "a@b.com"))));
        assertEquals("CUSTOMER", UserHeaderRelayFilter.role(jwt(Map.of("realm_access",
                Map.of("roles", List.of("offline_access"))))));
    }
}
