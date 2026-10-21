package com.mv.bookingservice.api.exception;

import com.mv.bookingservice.application.exception.DependencyUnavailableException;
import com.mv.bookingservice.infrastructure.client.FeignErrors;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Framework errors (validation, bad JSON, missing header / parameter, wrong method, optimistic lock...) with
 * Vietnamese messages, the same for every service. Runs before {@link GlobalExceptionHandler}, which keeps the
 * business exceptions of this service.
 * <p>
 * Body: { success: false, status, error, message, path, errors (field -&gt; message), timestamp }.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class CommonExceptionHandler {

    static final String GENERIC_ERROR = "Hệ thống đang gặp sự cố, vui lòng thử lại sau";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors().forEach(e -> errors.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
        return bad(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ", request, errors);
    }

    /** Constraints on @RequestParam / @PathVariable. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> invalidParameters(HandlerMethodValidationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(e -> errors.putIfAbsent(name != null ? name : "param", e.getDefaultMessage()));
        });
        return bad(HttpStatus.BAD_REQUEST, "Tham số không hợp lệ", request, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return bad(HttpStatus.BAD_REQUEST,
                "Dữ liệu gửi lên không đúng định dạng (JSON sai cú pháp, sai kiểu dữ liệu hoặc giá trị không hợp lệ)",
                request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "?";
        String hint = switch (expected) {
            case "UUID" -> " (cần UUID)";
            case "Integer", "int", "Long", "long" -> " (cần số nguyên)";
            case "BigDecimal" -> " (cần số)";
            case "Instant", "LocalDate", "LocalDateTime" -> " (sai định dạng ngày giờ)";
            default -> "";
        };
        return bad(HttpStatus.BAD_REQUEST,
                "Tham số '" + ex.getName() + "' không hợp lệ: '" + ex.getValue() + "'" + hint, request, null);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> missingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        if ("X-User-Id".equalsIgnoreCase(ex.getHeaderName())) {
            return bad(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để thực hiện thao tác này", request, null);
        }
        return bad(HttpStatus.BAD_REQUEST, "Thiếu header bắt buộc: " + ex.getHeaderName(), request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> missingParameter(MissingServletRequestParameterException ex,
                                                                HttpServletRequest request) {
        return bad(HttpStatus.BAD_REQUEST, "Thiếu tham số bắt buộc: " + ex.getParameterName(), request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> methodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                HttpServletRequest request) {
        return bad(HttpStatus.METHOD_NOT_ALLOWED,
                "Phương thức " + ex.getMethod() + " không được hỗ trợ cho đường dẫn này", request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> mediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type không được hỗ trợ, hãy dùng application/json",
                request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> noResource(NoResourceFoundException ex, HttpServletRequest request) {
        return bad(HttpStatus.NOT_FOUND, "Không tìm thấy đường dẫn " + request.getRequestURI(), request, null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> optimisticLock(ObjectOptimisticLockingFailureException ex,
                                                              HttpServletRequest request) {
        return bad(HttpStatus.CONFLICT, "Dữ liệu vừa được thay đổi bởi thao tác khác, vui lòng tải lại và thử lại",
                request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return bad(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc vi phạm ràng buộc", request, null);
    }

    /** A dependency (other service) is down, too slow, or its circuit breaker is OPEN. */
    @ExceptionHandler(DependencyUnavailableException.class)
    public ResponseEntity<Map<String, Object>> dependencyUnavailable(DependencyUnavailableException ex,
                                                                     HttpServletRequest request) {
        log.warn("Dependency unavailable on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After", "20")
                .body(body(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request, null));
    }

    /**
     * Feign error that an adapter did not translate. Business answers (400 / 404 / 409) keep the status and the
     * Vietnamese message of the other service; everything else means the other service cannot serve now (503).
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> feign(FeignException ex, HttpServletRequest request) {
        int status = ex.status();
        String remote = FeignErrors.message(ex);
        // 401 / 403 from the other service (caller unknown / not allowed there): same answer for the user.
        if (status == 400 || status == 401 || status == 403 || status == 404 || status == 409 || status == 422) {
            HttpStatus http = HttpStatus.valueOf(status);
            return ResponseEntity.status(http).body(body(http,
                    remote != null ? remote : "Yêu cầu tới dịch vụ liên quan không hợp lệ", request, null));
        }
        log.warn("Feign call to {} failed (HTTP {}): {}", FeignErrors.target(ex), status, ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After", "20")
                .body(body(HttpStatus.SERVICE_UNAVAILABLE,
                        "Dịch vụ liên quan tạm thời không khả dụng, vui lòng thử lại sau", request, null));
    }

    private static ResponseEntity<Map<String, Object>> bad(HttpStatus status, String message, HttpServletRequest request,
                                                           Map<String, String> errors) {
        return ResponseEntity.status(status).body(body(status, message, request, errors));
    }

    static Map<String, Object> body(HttpStatus status, String message, HttpServletRequest request,
                                    Map<String, String> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        if (errors != null && !errors.isEmpty()) {
            body.put("errors", errors);
        }
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
