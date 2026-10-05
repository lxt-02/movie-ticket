package com.mv.userservice.api.rest;

import com.mv.userservice.api.dto.response.ApiResponse;
import com.mv.userservice.api.dto.response.UserResponse;
import com.mv.userservice.api.dto.response.UserValidationResponse;
import com.mv.userservice.api.mapper.UserDtoMapper;
import com.mv.userservice.application.port.in.GetUserUseCase;
import com.mv.userservice.application.query.GetUserQuery;
import com.mv.userservice.domain.model.user.aggregate.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final GetUserUseCase getUserUseCase;
    private final UserDtoMapper userDtoMapper;

    /**
     * Internal endpoint to get user profile by ID.
     * Contract: GET /internal/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable("id") UUID id) {
        GetUserQuery query = GetUserQuery.builder().userId(id).build();
        User user = getUserUseCase.getById(query);
        UserResponse response = userDtoMapper.toResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Internal endpoint called by Booking Service to validate user account status (ACTIVE vs BLOCKED).
     * Contract: GET /internal/users/{id}/validation
     */
    @GetMapping("/{id}/validation")
    public ResponseEntity<ApiResponse<UserValidationResponse>> validateUser(@PathVariable("id") UUID id) {
        GetUserQuery query = GetUserQuery.builder().userId(id).build();
        User user = getUserUseCase.validateUser(query);
        UserValidationResponse response = userDtoMapper.toValidationResponse(user);
        return ResponseEntity.ok(ApiResponse.success("User is valid and active", response));
    }
}
