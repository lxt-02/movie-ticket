package com.mv.bookingservice.infrastructure.adapter;

import com.mv.bookingservice.application.port.out.UserClientPort;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import com.mv.bookingservice.infrastructure.client.UserFeignClient;
import com.mv.bookingservice.infrastructure.client.dto.RemoteResponse;
import com.mv.bookingservice.infrastructure.client.dto.UserValidationResponse;
import com.mv.bookingservice.infrastructure.resilience.ResilienceConfig;
import com.mv.bookingservice.infrastructure.resilience.RemoteCallGuard;
import feign.FeignException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Feign call to user-service behind the "user" circuit breaker (see {@link ShowtimeClientAdapter}). */
@Component
public class UserClientAdapter implements UserClientPort {

    private static final String SERVICE = "user-service";

    private final UserFeignClient userFeignClient;
    private final RemoteCallGuard guard;

    /** Retry → CircuitBreaker → Bulkhead, all named "user". */
    @Autowired
    public UserClientAdapter(UserFeignClient userFeignClient, CircuitBreakerRegistry breakers, RetryRegistry retries,
                 BulkheadRegistry bulkheads) {
        this.userFeignClient = userFeignClient;
        this.guard = new RemoteCallGuard(SERVICE, breakers.circuitBreaker(ResilienceConfig.USER),
                retries.retry(ResilienceConfig.USER), bulkheads.bulkhead(ResilienceConfig.USER));
    }

    /** Circuit breaker only (unit tests of the breaker). */
    public UserClientAdapter(UserFeignClient userFeignClient, CircuitBreakerRegistry breakers) {
        this.userFeignClient = userFeignClient;
        this.guard = RemoteCallGuard.breakerOnly(SERVICE, breakers.circuitBreaker(ResilienceConfig.USER));
    }

    /** Any combination, for tests. */
    public UserClientAdapter(UserFeignClient userFeignClient, RemoteCallGuard guard) {
        this.userFeignClient = userFeignClient;
        this.guard = guard;
    }

    @Override
    public UserValidationResult validateUser(UUID userId) {
        return guard.call(() -> {
            RemoteResponse<UserValidationResponse> response;
            try {
                response = userFeignClient.validateUser(userId);
            } catch (FeignException e) {
                if (e.status() == 404) {
                    // Profiles are created by GET /api/v1/users/me after the first Keycloak login.
                    throw new BookingValidationException(
                            "Chưa có hồ sơ người dùng, hãy mở trang cá nhân (GET /api/v1/users/me) sau khi đăng nhập rồi thử lại");
                }
                throw e;
            }
            UserValidationResponse data = requireData(response);
            return new UserValidationResult(
                    data.getUserId(),
                    data.getEmail(),
                    data.getFullName(),
                    data.getStatus()
            );
        });
    }

    private UserValidationResponse requireData(RemoteResponse<UserValidationResponse> response) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            String message = response != null ? response.getMessage() : "empty response";
            throw new IllegalStateException("User service failed to validate user: " + message);
        }
        return response.getData();
    }
}
