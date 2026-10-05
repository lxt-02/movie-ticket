package com.mv.bookingservice.api.rest;

import com.mv.bookingservice.api.dto.response.ApiResponse;
import com.mv.bookingservice.api.dto.response.BookingPaymentContextResponse;
import com.mv.bookingservice.api.mapper.BookingDtoMapper;
import com.mv.bookingservice.application.port.in.GetBookingPaymentContextUseCase;
import com.mv.bookingservice.application.query.GetBookingPaymentContextQuery;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/bookings")
@RequiredArgsConstructor
public class InternalBookingController {

    private final GetBookingPaymentContextUseCase getBookingPaymentContextUseCase;
    private final BookingDtoMapper bookingDtoMapper;

    /**
     * Endpoint called by Payment Service before initializing payment.
     * Contract: GET /internal/bookings/{id}/payment-context
     * Checks booking status == PENDING, not expired, verifies user ownership if requested, and returns amount + currency.
     */
    @GetMapping("/{id}/payment-context")
    public ResponseEntity<ApiResponse<BookingPaymentContextResponse>> getPaymentContext(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @RequestParam(value = "userId", required = false) UUID paramUserId
    ) {
        UUID userId = headerUserId != null ? headerUserId : paramUserId;
        GetBookingPaymentContextQuery query = GetBookingPaymentContextQuery.builder()
                .bookingId(id)
                .userId(userId)
                .build();
        Booking booking = getBookingPaymentContextUseCase.execute(query);
        BookingPaymentContextResponse response = bookingDtoMapper.toPaymentContextResponse(booking);
        return ResponseEntity.ok(ApiResponse.success("Booking payment context retrieved", response));
    }
}
