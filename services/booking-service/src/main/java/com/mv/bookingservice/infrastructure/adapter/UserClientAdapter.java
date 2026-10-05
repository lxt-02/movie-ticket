package com.mv.bookingservice.infrastructure.adapter;

import com.mv.bookingservice.application.port.out.UserClientPort;
import com.mv.bookingservice.infrastructure.client.UserFeignClient;
import com.mv.bookingservice.infrastructure.client.dto.ApiResponse;
import com.mv.bookingservice.infrastructure.client.dto.UserValidationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserClientAdapter implements UserClientPort {

    private final UserFeignClient userFeignClient;

    @Override
    public UserValidationResult validateUser(UUID userId) {
        ApiResponse<UserValidationResponse> response = userFeignClient.validateUser(userId);
        UserValidationResponse data = requireData(response);
        return new UserValidationResult(
                data.getUserId(),
                data.getEmail(),
                data.getFullName(),
                data.getStatus()
        );
    }

    private UserValidationResponse requireData(ApiResponse<UserValidationResponse> response) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            String message = response != null ? response.getMessage() : "empty response";
            throw new IllegalStateException("User service failed to validate user: " + message);
        }
        return response.getData();
    }
}
