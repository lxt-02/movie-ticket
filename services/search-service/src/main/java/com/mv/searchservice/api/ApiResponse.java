package com.mv.searchservice.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Same envelope as the other services: { success, message, data, timestamp }. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Thành công", data, Instant.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }
}
