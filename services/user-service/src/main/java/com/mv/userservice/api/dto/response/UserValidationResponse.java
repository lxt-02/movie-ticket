package com.mv.userservice.api.dto.response;

import com.mv.userservice.domain.model.user.enums.UserRole;
import com.mv.userservice.domain.model.user.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserValidationResponse {
    private UUID userId;
    private String email;
    private String fullName;
    private UserRole role;
    private UserStatus status;
    private boolean valid;
}
