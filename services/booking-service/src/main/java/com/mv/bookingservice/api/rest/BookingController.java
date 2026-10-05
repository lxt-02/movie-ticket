package com.mv.bookingservice.api.rest;

import com.mv.bookingservice.api.dto.request.CancelBookingRequest;
import com.mv.bookingservice.api.dto.request.CreateBookingRequest;
import com.mv.bookingservice.api.dto.response.ApiResponse;
import com.mv.bookingservice.api.dto.response.BookingResponse;
import com.mv.bookingservice.api.mapper.BookingDtoMapper;
import com.mv.bookingservice.application.command.CancelBookingCommand;
import com.mv.bookingservice.application.command.CreateBookingCommand;
import com.mv.bookingservice.application.port.in.CancelBookingUseCase;
import com.mv.bookingservice.application.port.in.CreateBookingUseCase;
import com.mv.bookingservice.application.port.in.GetBookingUseCase;
import com.mv.bookingservice.application.query.GetBookingByCodeQuery;
import com.mv.bookingservice.application.query.GetBookingQuery;
import com.mv.bookingservice.application.query.GetUserBookingsQuery;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final CreateBookingUseCase createBookingUseCase;
    private final GetBookingUseCase getBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final BookingDtoMapper bookingDtoMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        CreateBookingCommand command = bookingDtoMapper.toCommand(request, headerUserId, idempotencyKey);
        Booking booking = createBookingUseCase.execute(command);
        BookingResponse response = bookingDtoMapper.toResponse(booking);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Booking created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        GetBookingQuery query = GetBookingQuery.builder()
                .bookingId(id)
                .userId(headerUserId)
                .build();
        Booking booking = getBookingUseCase.getById(query);
        BookingResponse response = bookingDtoMapper.toResponse(booking);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/code/{bookingCode}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByCode(
            @PathVariable String bookingCode
    ) {
        GetBookingByCodeQuery query = GetBookingByCodeQuery.builder()
                .bookingCode(bookingCode)
                .build();
        Booking booking = getBookingUseCase.getByCode(query);
        BookingResponse response = bookingDtoMapper.toResponse(booking);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getUserBookings(
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @RequestParam(value = "userId", required = false) UUID paramUserId
    ) {
        UUID userId = headerUserId != null ? headerUserId : paramUserId;
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided via X-User-Id header or userId query parameter");
        }

        GetUserBookingsQuery query = GetUserBookingsQuery.builder()
                .userId(userId)
                .build();
        List<Booking> bookings = getBookingUseCase.getByUserId(query);
        List<BookingResponse> responses = bookings.stream()
                .map(bookingDtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID headerUserId,
            @RequestBody(required = false) CancelBookingRequest request
    ) {
        CancelBookingCommand command = CancelBookingCommand.builder()
                .bookingId(id)
                .userId(headerUserId)
                .reason(request != null ? request.getReason() : "Cancelled by user")
                .build();
        Booking booking = cancelBookingUseCase.execute(command);
        BookingResponse response = bookingDtoMapper.toResponse(booking);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", response));
    }
}
