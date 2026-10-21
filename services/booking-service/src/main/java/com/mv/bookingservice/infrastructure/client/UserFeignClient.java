package com.mv.bookingservice.infrastructure.client;

import com.mv.bookingservice.infrastructure.client.dto.RemoteResponse;
import com.mv.bookingservice.infrastructure.client.dto.UserValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/** Feign client of user-service (endpoint of user's InternalUserController). */
@FeignClient(name = "user-service", url = "${clients.user.base-url:}")
public interface UserFeignClient {

    /** 200 if the user exists and is ACTIVE; 403 if BLOCKED; 404 if there is no profile yet. */
    @GetMapping("/internal/users/{id}/validation")
    RemoteResponse<UserValidationResponse> validateUser(@PathVariable("id") UUID id);
}
