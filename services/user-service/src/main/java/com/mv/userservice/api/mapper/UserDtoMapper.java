package com.mv.userservice.api.mapper;

import com.mv.userservice.api.dto.response.UserResponse;
import com.mv.userservice.api.dto.response.UserValidationResponse;
import com.mv.userservice.domain.model.user.aggregate.User;
import org.springframework.stereotype.Component;

@Component
public class UserDtoMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public UserValidationResponse toValidationResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserValidationResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .valid(user.isActive())
                .build();
    }
}
