package com.mv.userservice.api.rest;

import com.mv.userservice.api.dto.response.ApiResponse;
import com.mv.userservice.api.dto.response.UserValidationResponse;
import com.mv.userservice.api.dto.response.UserResponse;
import com.mv.userservice.api.mapper.UserDtoMapper;
import com.mv.userservice.application.port.in.GetUserUseCase;
import com.mv.userservice.application.query.GetUserQuery;
import com.mv.userservice.domain.model.user.aggregate.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Internal user API (service-to-service). {@link #validateUser} is called by booking-service (its own UserFeignClient). */
@RestController
@RequiredArgsConstructor
public class InternalUserController {

    private final GetUserUseCase getUserUseCase;
    private final UserDtoMapper userDtoMapper;

    /** Profile by id (not used by other services at the moment). */
    @GetMapping("/internal/users/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable("id") UUID id) {
        User user = getUserUseCase.getById(GetUserQuery.builder().userId(id).build());
        return ApiResponse.success(userDtoMapper.toResponse(user));
    }

    /** Called by booking-service before a booking: ACTIVE user only (BLOCKED → 403, unknown → 404). */
    @GetMapping("/internal/users/{id}/validation")
    public ApiResponse<UserValidationResponse> validateUser(@PathVariable("id") UUID id) {
        User user = getUserUseCase.validateUser(GetUserQuery.builder().userId(id).build());
        return ApiResponse.success("Tài khoản hợp lệ", userDtoMapper.toValidationResponse(user));
    }
}
