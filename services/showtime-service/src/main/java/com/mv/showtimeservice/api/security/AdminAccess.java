package com.mv.showtimeservice.api.security;

import com.mv.showtimeservice.infrastructure.security.CurrentUser;

/**
 * Admin check for writes: the role comes from the {@code SecurityContextHolder} (built from the X-User-Role header
 * that the gateway takes from the Keycloak token).
 */
public final class AdminAccess {

    public static final String ADMIN = "ADMIN";

    private AdminAccess() {
    }

    public static boolean isAdmin() {
        return CurrentUser.hasRole(ADMIN);
    }

    public static void require() {
        if (!isAdmin()) {
            throw new ForbiddenException("Chỉ ADMIN mới được quản lý suất chiếu");
        }
    }
}
