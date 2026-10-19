package com.mv.bookingservice.infrastructure.adapter;

import com.mv.bookingservice.application.port.out.UserClientPort;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import com.mv.bookingservice.infrastructure.client.UserFeignClient;
import com.mv.bookingservice.infrastructure.client.dto.ApiResponse;
import com.mv.bookingservice.infrastructure.client.dto.UserValidationResponse;
import com.mv.bookingservice.infrastructure.resilience.ResilienceConfig;
import com.mv.bookingservice.infrastructure.resilience.ResilientCall;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Feign call to user-service behind the "user" circuit breaker (see {@link ShowtimeClientAdapter}). */
@Component
public class UserClientAdapter implements UserClientPort {

    private static final String SERVICE = "user-service";

    private final UserFeignClient userFeignClient;
    private final CircuitBreaker circuitBreaker;

    public UserClientAdapter(UserFeignClient userFeignClient, CircuitBreakerRegistry registry) {
        this.userFeignClient = userFeignClient;
        this.circuitBreaker = registry.circuitBreaker(ResilienceConfig.USER);
    }

    @Override
    public UserValidationResult validateUser(UUID userId) {
        return ResilientCall.run(circuitBreaker, SERVICE, () -> {
            ApiResponse<UserValidationResponse> response;
            try {
                response = userFeignClient.validateUser(userId);
            } catch (FeignException e) {
                if (e.status() == 404) {
                    throw new BookingValidationException("User " + userId + " not found");
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

    private UserValidationResponse requireData(ApiResponse<UserValidationResponse> response) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            String message = response != null ? response.getMessage() : "empty response";
            throw new IllegalStateException("User service failed to validate user: " + message);
        }
        return response.getData();
    }
}
