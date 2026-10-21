package com.mv.userservice.api.rest;

import com.mv.userservice.api.dto.request.UpdateProfileRequest;
import com.mv.userservice.api.dto.request.UserStatusRequest;
import com.mv.userservice.api.dto.response.ApiResponse;
import com.mv.userservice.api.dto.response.PageResponse;
import com.mv.userservice.api.dto.response.UserResponse;
import com.mv.userservice.api.mapper.UserDtoMapper;
import com.mv.userservice.api.security.ForbiddenException;
import com.mv.userservice.infrastructure.security.CurrentUser;
import com.mv.userservice.application.dto.PageResult;
import com.mv.userservice.application.port.in.GetUserUseCase;
import com.mv.userservice.application.port.in.ManageProfileUseCase;
import com.mv.userservice.application.query.GetUserQuery;
import com.mv.userservice.domain.model.user.aggregate.User;
import com.mv.userservice.domain.model.user.enums.UserRole;
import com.mv.userservice.domain.model.user.enums.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Profile of the caller and admin user management. Login / registration happen in Keycloak; the gateway checks
 * the token and sets X-User-Id (token "sub"), X-User-Role, X-User-Email and X-User-Name (URL-encoded).
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {


    private final GetUserUseCase getUserUseCase;
    private final ManageProfileUseCase manageProfileUseCase;
    private final UserDtoMapper userDtoMapper;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Name", required = false) String name
    ) {
        User user = manageProfileUseCase.currentUser(userId, decode(email), decode(name), toRole(CurrentUser.role().orElse(null)));
        return ResponseEntity.ok(ApiResponse.success(userDtoMapper.toResponse(user)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        User user = manageProfileUseCase.updateProfile(userId, request.getFullName(), request.getAvatarUrl());
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userDtoMapper.toResponse(user)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> search(
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        requireAdmin();
        PageResult<User> result = manageProfileUseCase.search(keyword, role, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(new PageResponse<>(
                result.items().stream().map(userDtoMapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(
            @PathVariable UUID id
    ) {
        requireAdmin();
        User user = getUserUseCase.getById(GetUserQuery.builder().userId(id).build());
        return ResponseEntity.ok(ApiResponse.success(userDtoMapper.toResponse(user)));
    }

    /** Block / unblock an account. A BLOCKED user cannot create bookings. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UserStatusRequest request
    ) {
        requireAdmin();
        User user = manageProfileUseCase.changeStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("User status changed", userDtoMapper.toResponse(user)));
    }

    private static String decode(String value) {
        return value == null ? null : URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static UserRole toRole(String role) {
        if (role == null || role.isBlank()) {
            return UserRole.CUSTOMER;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserRole.CUSTOMER;
        }
    }

    /** Role from the SecurityContextHolder (built from the gateway header X-User-Role). */
    private static void requireAdmin() {
        if (!CurrentUser.hasRole("ADMIN")) {
            throw new ForbiddenException("Chỉ ADMIN mới được quản lý người dùng");
        }
    }
}
