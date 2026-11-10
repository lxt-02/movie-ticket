package com.mv.userservice.application.port.out;

import com.mv.userservice.domain.model.user.aggregate.User;

import java.util.Optional;
import java.util.UUID;

public interface LoadUserPort {
    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    com.mv.userservice.application.dto.PageResult<User> search(String keyword,
            com.mv.userservice.domain.model.user.enums.UserRole role,
            com.mv.userservice.domain.model.user.enums.UserStatus status, int page, int size);
}
