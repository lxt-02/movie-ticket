package com.mv.userservice.application.service;

import com.mv.userservice.application.dto.PageResult;
import com.mv.userservice.application.port.in.ManageProfileUseCase;
import com.mv.userservice.application.port.out.LoadUserPort;
import com.mv.userservice.application.port.out.SaveUserPort;
import com.mv.userservice.domain.model.user.aggregate.User;
import com.mv.userservice.domain.model.user.enums.UserRole;
import com.mv.userservice.domain.model.user.enums.UserStatus;
import com.mv.userservice.domain.model.user.exception.InvalidUserException;
import com.mv.userservice.domain.model.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ManageProfileService implements ManageProfileUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    @Override
    public User currentUser(UUID userId, String email, String fullName, UserRole role) {
        Instant now = Instant.now();
        UserRole tokenRole = role == null ? UserRole.CUSTOMER : role;
        User existing = loadUserPort.findById(userId).orElse(null);
        if (existing != null) {
            if (existing.getRole() != tokenRole) {
                existing.setRole(tokenRole);
                existing.setUpdatedAt(now);
                return saveUserPort.save(existing);
            }
            return existing;
        }
        if (email == null || email.isBlank()) {
            throw new InvalidUserException("Tài khoản đăng nhập chưa có email nên không tạo được hồ sơ");
        }
        loadUserPort.findByEmail(email.trim()).ifPresent(other -> {
            throw new InvalidUserException("Email " + email + " đã thuộc về tài khoản khác");
        });
        String name = fullName == null || fullName.isBlank() ? email.trim() : fullName.trim();
        User created = User.builder()
                .id(userId)
                .email(email.trim())
                .fullName(name.length() > 150 ? name.substring(0, 150) : name)
                .role(tokenRole)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        log.info("Created profile for Keycloak user {} ({})", userId, created.getEmail());
        return saveUserPort.save(created);
    }

    @Override
    public User updateProfile(UUID userId, String fullName, String avatarUrl) {
        User user = load(userId);
        user.updateProfile(fullName, avatarUrl, Instant.now());
        return saveUserPort.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<User> search(String keyword, UserRole role, UserStatus status, int page, int size) {
        String q = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return loadUserPort.search(q, role, status, Math.max(0, page), Math.min(100, Math.max(1, size)));
    }

    @Override
    public User changeStatus(UUID userId, UserStatus status) {
        User user = load(userId);
        user.changeStatus(status);
        return saveUserPort.save(user);
    }

    private User load(UUID userId) {
        return loadUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }
}
