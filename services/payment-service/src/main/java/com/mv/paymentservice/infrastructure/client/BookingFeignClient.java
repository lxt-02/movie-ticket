package com.mv.paymentservice.infrastructure.client;

import com.mv.paymentservice.api.dto.response.ApiResponse;
import com.mv.paymentservice.application.dto.BookingPaymentContextDto;
import com.mv.paymentservice.infrastructure.client.dto.ConfirmBookingPaymentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "booking-service", url = "${clients.booking.base-url:}")
public interface BookingFeignClient {

    @GetMapping("/internal/bookings/{id}/payment-context")
    ApiResponse<BookingPaymentContextDto> getPaymentContext(
            @PathVariable("id") UUID bookingId,
            @RequestHeader("X-User-Id") UUID userId
    );

    @PostMapping("/internal/bookings/{id}/payment-succeeded")
    ApiResponse<Object> confirmPaymentSucceeded(
            @PathVariable("id") UUID bookingId,
            @RequestBody ConfirmBookingPaymentRequest request
    );
}
