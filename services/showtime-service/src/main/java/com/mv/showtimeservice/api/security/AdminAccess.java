package com.mv.showtimeservice.api.security;

/**
 * Admin check for showtime writes. Until JWT is in place the role comes from the X-User-Role header
 * (same trust model as X-User-Id): only "ADMIN" may create, change or cancel showtimes.
 */
public final class AdminAccess {

    public static final String ROLE_HEADER = "X-User-Role";
    public static final String ADMIN = "ADMIN";

    private AdminAccess() {
    }

    public static boolean isAdmin(String role) {
        return role != null && ADMIN.equalsIgnoreCase(role.trim());
    }

    public static void require(String role) {
        if (!isAdmin(role)) {
            throw new ForbiddenException("Only ADMIN can manage showtimes (header " + ROLE_HEADER + ")");
        }
    }
}
