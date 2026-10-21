package com.mv.userservice.domain.model.user.exception;

import java.util.UUID;

public class UserBlockedException extends DomainException {
    public UserBlockedException(UUID userId) {
        super("Tài khoản đã bị khoá hoặc ngừng hoạt động: " + userId);
    }
}
