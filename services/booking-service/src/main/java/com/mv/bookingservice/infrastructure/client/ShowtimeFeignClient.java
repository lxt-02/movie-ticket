package com.mv.bookingservice.infrastructure.client;

import com.mv.bookingservice.infrastructure.client.dto.HoldSeatsRequest;
import com.mv.bookingservice.infrastructure.client.dto.RemoteResponse;
import com.mv.bookingservice.infrastructure.client.dto.SeatHoldResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

/**
 * Feign client of showtime-service (seat holds). Endpoints of showtime's InternalShowtimeController;
 * request/response classes are booking's own copies in {@code infrastructure.client.dto}.
 * Full paths on each method (no class-level @RequestMapping: Feign does not allow it on a client interface).
 */
@FeignClient(name = "showtime-service", url = "${clients.showtime.base-url:}")
public interface ShowtimeFeignClient {

    /** Holds seats for a booking (idempotent per bookingId). 201, or 409 if a seat is taken. */
    @PostMapping("/internal/showtimes/{id}/holds")
    RemoteResponse<SeatHoldResponse> holdSeats(@PathVariable("id") UUID showtimeId,
                                               @RequestBody HoldSeatsRequest request);

    @GetMapping("/internal/holds/by-booking/{bookingId}")
    RemoteResponse<SeatHoldResponse> getHoldByBookingId(@PathVariable("bookingId") UUID bookingId);

    /** Gives the held seats back (booking cancelled / expired). */
    @PostMapping("/internal/holds/{id}/release")
    RemoteResponse<SeatHoldResponse> releaseHold(@PathVariable("id") UUID holdId);

    /** Held seats become BOOKED after payment. 409 if the hold is no longer valid. */
    @PostMapping("/internal/holds/{id}/confirm")
    RemoteResponse<SeatHoldResponse> confirmHold(@PathVariable("id") UUID holdId);
}
