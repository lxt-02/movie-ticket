package com.mv.userservice.application.port.out;

import com.mv.userservice.domain.model.user.aggregate.User;

import java.util.Optional;
import java.util.UUID;

public interface LoadUserPort {
    Optional<User> findById(UUID id);
}
