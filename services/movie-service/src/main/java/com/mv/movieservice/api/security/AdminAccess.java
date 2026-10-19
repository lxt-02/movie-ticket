package com.mv.movieservice.api.security;

/**
 * Admin check for catalogue writes. Until JWT is in place the role comes from the X-User-Role header
 * (same trust model as X-User-Id): only "ADMIN" may create, edit or archive.
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
            throw new ForbiddenException("Only ADMIN can manage movies (header " + ROLE_HEADER + ")");
        }
    }
}
