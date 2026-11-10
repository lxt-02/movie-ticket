package com.mv.paymentservice.api.rest;

import com.mv.paymentservice.api.dto.response.BookingPaymentStatusResponse;
import com.mv.paymentservice.api.dto.response.ApiResponse;
import com.mv.paymentservice.api.dto.response.PaymentResponse;
import com.mv.paymentservice.api.mapper.PaymentDtoMapper;
import com.mv.paymentservice.application.port.in.GetPaymentUseCase;
import com.mv.paymentservice.application.query.GetPaymentQuery;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Internal payment API (service-to-service). paid-status is called by booking-service (its own PaymentFeignClient). */
@RestController
@RequiredArgsConstructor
public class InternalPaymentController {

    private final GetPaymentUseCase getPaymentUseCase;
    private final PaymentDtoMapper paymentDtoMapper;

    @GetMapping("/internal/payments/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable UUID id) {
        Payment payment = getPaymentUseCase.getPaymentById(new GetPaymentQuery(id));
        return ResponseEntity.ok(ApiResponse.success(paymentDtoMapper.toResponse(payment)));
    }

    @GetMapping("/internal/payments/by-booking/{bookingId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByBookingId(@PathVariable UUID bookingId) {
        List<Payment> payments = getPaymentUseCase.getPaymentsByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.success(paymentDtoMapper.toResponseList(payments)));
    }

    /**
     * Safety net for booking-service: is this booking paid? Used before an unpaid booking is expired, so a
     * PAYMENT_SUCCEEDED event lost on the way (or parked in the dead-letter topic) cannot cost a paid seat.
     */
    @GetMapping("/internal/payments/by-booking/{bookingId}/paid-status")
    public ApiResponse<BookingPaymentStatusResponse> getBookingPaymentStatus(@PathVariable("bookingId") UUID bookingId) {
        Optional<Payment> paid = getPaymentUseCase.getPaymentsByBookingId(bookingId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .findFirst();
        BookingPaymentStatusResponse.BookingPaymentStatusResponseBuilder status = BookingPaymentStatusResponse.builder()
                .bookingId(bookingId)
                .paid(paid.isPresent());
        paid.ifPresent(p -> status
                .paymentId(p.getId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .paidAt(p.getPaidAt()));
        return ApiResponse.success(status.build());
    }
}
