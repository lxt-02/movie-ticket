package com.mv.cinemaservice.api.security;

import com.mv.cinemaservice.infrastructure.security.CurrentUser;

public final class AdminAccess {

    public static final String ADMIN = "ADMIN";

    private AdminAccess() {
    }

    public static boolean isAdmin() {
        return CurrentUser.hasRole(ADMIN);
    }

    public static void require() {
        if (!isAdmin()) {
            throw new ForbiddenException("Chỉ ADMIN mới được quản lý rạp");
        }
    }
}
