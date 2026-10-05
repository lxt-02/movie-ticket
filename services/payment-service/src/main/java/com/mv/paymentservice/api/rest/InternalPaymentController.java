package com.mv.paymentservice.api.rest;

import com.mv.paymentservice.api.dto.response.ApiResponse;
import com.mv.paymentservice.api.dto.response.PaymentResponse;
import com.mv.paymentservice.api.mapper.PaymentDtoMapper;
import com.mv.paymentservice.application.port.in.GetPaymentUseCase;
import com.mv.paymentservice.application.query.GetPaymentQuery;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final GetPaymentUseCase getPaymentUseCase;
    private final PaymentDtoMapper paymentDtoMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable UUID id) {
        Payment payment = getPaymentUseCase.getPaymentById(new GetPaymentQuery(id));
        return ResponseEntity.ok(ApiResponse.success(paymentDtoMapper.toResponse(payment)));
    }

    @GetMapping("/by-booking/{bookingId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByBookingId(@PathVariable UUID bookingId) {
        List<Payment> payments = getPaymentUseCase.getPaymentsByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.success(paymentDtoMapper.toResponseList(payments)));
    }
}
