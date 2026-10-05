package com.mv.bookingservice.infrastructure.client;

import com.mv.bookingservice.infrastructure.client.dto.ApiResponse;
import com.mv.bookingservice.infrastructure.client.dto.HoldSeatsRequest;
import com.mv.bookingservice.infrastructure.client.dto.SeatHoldResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "showtime-service", url = "${clients.showtime.base-url:http://localhost:8082}")
public interface ShowtimeFeignClient {

    @PostMapping("/internal/showtimes/{id}/holds")
    ApiResponse<SeatHoldResponse> holdSeats(
            @PathVariable("id") UUID showtimeId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody HoldSeatsRequest request
    );

    @GetMapping("/internal/holds/by-booking/{bookingId}")
    ApiResponse<SeatHoldResponse> getHoldByBookingId(@PathVariable("bookingId") UUID bookingId);

    @PostMapping("/internal/holds/{id}/release")
    ApiResponse<SeatHoldResponse> releaseHold(@PathVariable("id") UUID holdId);

    @PostMapping("/internal/holds/{id}/confirm")
    ApiResponse<SeatHoldResponse> confirmHold(@PathVariable("id") UUID holdId);
}
