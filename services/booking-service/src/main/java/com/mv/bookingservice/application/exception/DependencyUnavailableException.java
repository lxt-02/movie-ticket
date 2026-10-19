package com.mv.bookingservice.application.exception;

/**
 * A service booking depends on (showtime, user) is down or its circuit breaker is OPEN. The request was not
 * executed; the caller should try again later (HTTP 503). Temporary by nature: jobs keep retrying.
 */
public class DependencyUnavailableException extends RuntimeException {
    public DependencyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
