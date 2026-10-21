package com.mv.userservice.domain.model.user.aggregate;

import com.mv.userservice.domain.model.user.enums.UserRole;
import com.mv.userservice.domain.model.user.enums.UserStatus;
import com.mv.userservice.domain.model.user.exception.InvalidUserException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private boolean emailVerified;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public void block() {
        this.status = UserStatus.BLOCKED;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /**
     * The user edits their own profile: name and avatar. Email, role and status are not editable here
     * (email belongs to login, role / status to admins).
     */
    public void updateProfile(String newFullName, String newAvatarUrl, Instant now) {
        String name = newFullName == null ? null : newFullName.trim();
        if (name == null || name.isEmpty()) {
            throw new InvalidUserException("Họ tên không được để trống");
        }
        if (name.length() > 150) {
            throw new InvalidUserException("Họ tên tối đa 150 ký tự");
        }
        String avatar = newAvatarUrl == null || newAvatarUrl.isBlank() ? null : newAvatarUrl.trim();
        if (avatar != null && !(avatar.startsWith("https://") || avatar.startsWith("http://"))) {
            throw new InvalidUserException("Ảnh đại diện phải là đường dẫn http(s)");
        }
        this.fullName = name;
        this.avatarUrl = avatar;
        this.updatedAt = now;
    }

    /** Admin: ACTIVE <-> BLOCKED. A blocked user cannot book (checked by booking-service). */
    public void changeStatus(UserStatus target) {
        if (target == null) {
            throw new InvalidUserException("Trạng thái là bắt buộc");
        }
        if (target == UserStatus.BLOCKED) {
            block();
        } else {
            activate();
        }
    }
}
