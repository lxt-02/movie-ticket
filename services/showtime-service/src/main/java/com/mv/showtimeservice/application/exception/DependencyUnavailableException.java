package com.mv.showtimeservice.application.exception;

/** movie-service / cinema-service cannot answer now (down, timeout, 5xx). HTTP 503. */
public class DependencyUnavailableException extends RuntimeException {
    public DependencyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
