package com.mv.bookingservice.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;

/** Reads the error body of another service ({ "message": "..." }) so its Vietnamese message can be shown. */
public final class FeignErrors {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FeignErrors() {
    }

    /** "message" of the JSON error body, or null if there is none. */
    public static String message(FeignException e) {
        try {
            String body = e.contentUTF8();
            if (body == null || body.isBlank()) {
                return null;
            }
            JsonNode message = MAPPER.readTree(body).get("message");
            return message != null && message.isTextual() && !message.asText().isBlank() ? message.asText() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String messageOr(FeignException e, String fallback) {
        String message = message(e);
        return message != null ? message : fallback;
    }

    /** "GET http://cinema-service/internal/..." for logs. */
    public static String target(FeignException e) {
        return e.request() != null ? e.request().httpMethod() + " " + e.request().url() : "?";
    }

    /** Other service down / too slow / 5xx. */
    public static boolean isUnavailable(FeignException e) {
        int status = e.status();
        return status < 0 || status >= 500;
    }
}
