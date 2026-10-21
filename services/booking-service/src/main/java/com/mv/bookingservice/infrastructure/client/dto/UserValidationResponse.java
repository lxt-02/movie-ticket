package com.mv.bookingservice.infrastructure.client.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Result of validateUser. role: CUSTOMER / STAFF / ADMIN, status: ACTIVE / BLOCKED. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserValidationResponse {
    private UUID userId;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private boolean valid;
}
