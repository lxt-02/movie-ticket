package com.mv.userservice.api.dto.response;

import com.mv.userservice.domain.model.user.enums.UserRole;
import com.mv.userservice.domain.model.user.enums.UserStatus;
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
public class UserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private boolean emailVerified;
    private Instant createdAt;
    private Instant updatedAt;
}
