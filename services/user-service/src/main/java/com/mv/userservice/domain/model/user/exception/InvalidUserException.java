package com.mv.userservice.domain.model.user.exception;

/** A profile rule was broken (HTTP 400). */
public class InvalidUserException extends DomainException {
    public InvalidUserException(String message) {
        super(message);
    }
}
