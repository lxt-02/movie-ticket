package com.mv.userservice.domain;

import com.mv.userservice.domain.model.user.aggregate.User;
import com.mv.userservice.domain.model.user.enums.UserRole;
import com.mv.userservice.domain.model.user.enums.UserStatus;
import com.mv.userservice.domain.model.user.exception.InvalidUserException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserProfileTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");

    private static User user() {
        return User.builder().id(UUID.randomUUID()).email("a@b.com").fullName("Old").role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE).build();
    }

    @Test
    void updatesNameAndAvatarOnly() {
        User user = user();
        user.updateProfile("  Nguyen Van A ", " ", NOW);

        assertEquals("Nguyen Van A", user.getFullName());
        assertNull(user.getAvatarUrl());
        assertEquals("a@b.com", user.getEmail());
        assertEquals(UserRole.CUSTOMER, user.getRole());
        assertEquals(NOW, user.getUpdatedAt());
    }

    @Test
    void rejectsBlankNameAndNonHttpAvatar() {
        assertThrows(InvalidUserException.class, () -> user().updateProfile(" ", null, NOW));
        assertThrows(InvalidUserException.class, () -> user().updateProfile("A", "javascript:alert(1)", NOW));
    }

    @Test
    void adminCanBlockAndUnblock() {
        User user = user();
        user.changeStatus(UserStatus.BLOCKED);
        assertEquals(UserStatus.BLOCKED, user.getStatus());
        user.changeStatus(UserStatus.ACTIVE);
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }
}
