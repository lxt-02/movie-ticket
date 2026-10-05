package com.mv.userservice.domain.model.user.exception;

import java.util.UUID;

public class UserBlockedException extends DomainException {
    public UserBlockedException(UUID userId) {
        super("User is blocked or inactive: " + userId);
    }
}
