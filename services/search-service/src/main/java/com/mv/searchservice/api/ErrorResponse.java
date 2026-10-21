package com.mv.searchservice.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Same error body as the other services: { success: false, status, error, message, path, timestamp }. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success, int status, String error, String message, String path, Instant timestamp) {
}
