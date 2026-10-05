package com.mv.paymentservice.api.rest;

import com.mv.paymentservice.api.dto.request.CreatePaymentRequest;
import com.mv.paymentservice.api.dto.request.PaymentCallbackRequest;
import com.mv.paymentservice.api.dto.response.ApiResponse;
import com.mv.paymentservice.api.dto.response.PaymentResponse;
import com.mv.paymentservice.api.mapper.PaymentDtoMapper;
import com.mv.paymentservice.application.command.CreatePaymentCommand;
import com.mv.paymentservice.application.command.PaymentCallbackCommand;
import com.mv.paymentservice.application.port.in.CreatePaymentUseCase;
import com.mv.paymentservice.application.port.in.GetPaymentUseCase;
import com.mv.paymentservice.application.port.in.ProcessPaymentCallbackUseCase;
import com.mv.paymentservice.application.query.GetPaymentQuery;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final ProcessPaymentCallbackUseCase processPaymentCallbackUseCase;
    private final PaymentDtoMapper paymentDtoMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") UUID userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        String requestHash = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String rawPayload = userId + ":" + request.getBookingId() + ":" + request.getProvider();
            requestHash = sha256(rawPayload);
        }

        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingId(request.getBookingId())
                .userId(userId)
                .provider(request.getProvider())
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .build();

        Payment payment = createPaymentUseCase.createPayment(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated successfully", paymentDtoMapper.toResponse(payment)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable UUID id) {
        Payment payment = getPaymentUseCase.getPaymentById(new GetPaymentQuery(id));
        return ResponseEntity.ok(ApiResponse.success(paymentDtoMapper.toResponse(payment)));
    }

    @PostMapping("/{id}/callback")
    public ResponseEntity<ApiResponse<PaymentResponse>> handleCallback(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentCallbackRequest request
    ) {
        PaymentCallbackCommand command = PaymentCallbackCommand.builder()
                .paymentId(id)
                .providerTransactionId(request.getProviderTransactionId())
                .success(request.isSuccess())
                .failureReason(request.getFailureReason())
                .build();

        Payment payment = processPaymentCallbackUseCase.processCallback(command);
        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", paymentDtoMapper.toResponse(payment)));
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
