package com.mv.userservice.application.service;

import com.mv.userservice.application.port.out.LoadUserPort;
import com.mv.userservice.application.query.GetUserQuery;
import com.mv.userservice.domain.model.user.aggregate.User;
import com.mv.userservice.domain.model.user.exception.UserBlockedException;
import com.mv.userservice.domain.model.user.exception.UserNotFoundException;
import com.mv.userservice.domain.model.user.enums.UserRole;
import com.mv.userservice.domain.model.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    private GetUserService getUserService;

    @BeforeEach
    void setUp() {
        getUserService = new GetUserService(loadUserPort);
    }

    @Test
    void shouldReturnUserWhenFoundAndActive() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .fullName("Test User")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(loadUserPort.findById(userId)).thenReturn(Optional.of(user));

        User result = getUserService.validateUser(new GetUserQuery(userId));

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertTrue(result.isActive());
    }

    @Test
    void shouldThrowUserBlockedExceptionWhenUserBlocked() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("blocked@example.com")
                .fullName("Blocked User")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.BLOCKED)
                .build();

        when(loadUserPort.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(UserBlockedException.class, () -> getUserService.validateUser(new GetUserQuery(userId)));
    }

    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(loadUserPort.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> getUserService.getById(new GetUserQuery(userId)));
    }
}
