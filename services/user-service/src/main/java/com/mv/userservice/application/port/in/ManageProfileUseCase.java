package com.mv.userservice.application.port.in;

import com.mv.userservice.application.dto.PageResult;
import com.mv.userservice.domain.model.user.aggregate.User;
import com.mv.userservice.domain.model.user.enums.UserRole;
import com.mv.userservice.domain.model.user.enums.UserStatus;

import java.util.UUID;

public interface ManageProfileUseCase {
    /**
     * Profile of the logged-in Keycloak user. Created on the first call (id = token "sub");
     * the role always follows Keycloak.
     */
    User currentUser(UUID userId, String email, String fullName, UserRole role);

    User updateProfile(UUID userId, String fullName, String avatarUrl);

    /** Admin list: keyword matches email or name. */
    PageResult<User> search(String keyword, UserRole role, UserStatus status, int page, int size);

    /** Admin: block / unblock. */
    User changeStatus(UUID userId, UserStatus status);
}
