package com.mv.userservice.domain.repository;

import com.mv.userservice.domain.model.user.aggregate.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    User save(User user);
}
