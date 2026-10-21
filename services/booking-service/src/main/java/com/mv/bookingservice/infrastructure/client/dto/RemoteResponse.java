package com.mv.bookingservice.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Envelope of every answer of another service ({ success, message, data, timestamp }).
 * Client-side copy owned by this service: the remote service keeps its own ApiResponse.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RemoteResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Instant timestamp;

    public static <T> RemoteResponse<T> success(T data) {
        return new RemoteResponse<>(true, null, data, Instant.now());
    }
}
