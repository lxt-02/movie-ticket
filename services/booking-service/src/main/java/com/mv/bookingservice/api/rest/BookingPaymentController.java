package com.mv.bookingservice.api.rest;

import com.mv.bookingservice.api.dto.response.ApiResponse;
import com.mv.bookingservice.api.dto.response.BookingResponse;
import com.mv.bookingservice.api.mapper.BookingDtoMapper;
import com.mv.bookingservice.application.command.RetryBookingPaymentCommand;
import com.mv.bookingservice.application.port.in.RetryBookingPaymentUseCase;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingPaymentController {

    private final RetryBookingPaymentUseCase retryBookingPaymentUseCase;
    private final BookingDtoMapper bookingDtoMapper;

    /**
     * "Pay again" after a failed or cancelled payment. The new PayOS link is created asynchronously: poll
     * GET /api/v1/payments/by-booking/{id} until a payment newer than the previous one shows up.
     */
    @PostMapping("/{id}/payment-retry")
    public ResponseEntity<ApiResponse<BookingResponse>> retryPayment(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id") UUID headerUserId
    ) {
        Booking booking = retryBookingPaymentUseCase.execute(RetryBookingPaymentCommand.builder()
                .bookingId(id)
                .userId(headerUserId)
                .build());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("New payment link requested", bookingDtoMapper.toResponse(booking)));
    }
}
