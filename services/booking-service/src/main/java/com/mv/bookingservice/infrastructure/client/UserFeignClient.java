package com.mv.bookingservice.infrastructure.client;

import com.mv.bookingservice.infrastructure.client.dto.ApiResponse;
import com.mv.bookingservice.infrastructure.client.dto.UserValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${clients.user.base-url:}")
public interface UserFeignClient {

    @GetMapping("/internal/users/{id}/validation")
    ApiResponse<UserValidationResponse> validateUser(@PathVariable("id") UUID id);
}
