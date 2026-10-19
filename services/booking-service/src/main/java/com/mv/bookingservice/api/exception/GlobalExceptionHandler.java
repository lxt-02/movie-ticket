package com.mv.bookingservice.api.exception;

import com.mv.bookingservice.domain.model.booking.exception.BookingConflictException;
import com.mv.bookingservice.domain.model.booking.exception.BookingNotFoundException;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import com.mv.bookingservice.domain.model.booking.exception.InvalidBookingStateException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import com.mv.bookingservice.application.exception.DependencyUnavailableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** showtime / user service down, or its circuit breaker OPEN: nothing was done, try again later. */
    @ExceptionHandler(DependencyUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleDependencyUnavailable(DependencyUnavailableException ex,
                                                                     HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "20")
                .body(response);
    }

    /** Missing X-User-Id (or another required header): the caller is not identified. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        HttpStatus status = "X-User-Id".equalsIgnoreCase(ex.getHeaderName()) ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("Missing required header " + ex.getHeaderName())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(BookingNotFoundException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(BookingConflictException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({BookingValidationException.class, InvalidBookingStateException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid request payload")
                .errors(errors)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
