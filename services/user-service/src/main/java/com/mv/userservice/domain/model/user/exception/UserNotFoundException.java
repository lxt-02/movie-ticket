package com.mv.userservice.domain.model.user.exception;

import java.util.UUID;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(UUID userId) {
        super("User not found with id: " + userId);
    }
}
