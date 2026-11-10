package com.mv.bookingservice.application.port.out;

import java.util.UUID;

public interface UserClientPort {
    /**
     * Outbound contract to verify user details/status if needed.
     */
    UserValidationResult validateUser(UUID userId);

    record UserValidationResult(
            UUID userId,
            String email,
            String fullName,
            String status
    ) {}
}
