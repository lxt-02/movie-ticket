package com.mv.cinemaservice.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/** Reads the caller from the {@link SecurityContextHolder} (filled by {@link GatewayHeaderAuthenticationFilter}). */
public final class CurrentUser {

    public static final String ROLE_PREFIX = "ROLE_";

    private CurrentUser() {
    }

    public static Optional<UUID> id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID id)) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    /** ADMIN / STAFF / CUSTOMER, or empty for an anonymous request. */
    public static Optional<String> role() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID)) {
            return Optional.empty();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(ROLE_PREFIX))
                .map(a -> a.substring(ROLE_PREFIX.length()))
                .findFirst();
    }

    public static boolean hasRole(String role) {
        return role().map(role::equalsIgnoreCase).orElse(false);
    }
}
