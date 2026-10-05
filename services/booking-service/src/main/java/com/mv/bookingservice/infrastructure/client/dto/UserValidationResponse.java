package com.mv.bookingservice.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
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
