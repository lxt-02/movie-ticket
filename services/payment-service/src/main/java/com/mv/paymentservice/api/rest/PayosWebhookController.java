package com.mv.paymentservice.api.rest;

import com.mv.paymentservice.api.dto.response.ApiResponse;
import com.mv.paymentservice.application.port.in.HandlePayosWebhookUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint called by PayOS (register it on my.payos.vn). No auth: trust comes from the signature check.
 * The body is read as raw text so the signature is computed over exactly what PayOS sent.
 * 200 = accepted or ignored, 400 = invalid signature / mismatched data (see GlobalExceptionHandler).
 */
@RestController
@RequestMapping("/api/v1/payments/payos")
@RequiredArgsConstructor
public class PayosWebhookController {

    private final HandlePayosWebhookUseCase handlePayosWebhookUseCase;

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(@RequestBody String body) {
        handlePayosWebhookUseCase.handle(body);
        return ResponseEntity.ok(ApiResponse.<Void>success("OK", null));
    }
}
