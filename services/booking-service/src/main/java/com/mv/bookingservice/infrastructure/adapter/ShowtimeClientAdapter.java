package com.mv.bookingservice.infrastructure.adapter;

import com.mv.bookingservice.application.exception.SeatHoldRejectedException;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.exception.BookingConflictException;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import com.mv.bookingservice.infrastructure.client.ShowtimeFeignClient;
import com.mv.bookingservice.infrastructure.client.dto.ApiResponse;
import com.mv.bookingservice.infrastructure.client.dto.HeldSeatResponse;
import com.mv.bookingservice.infrastructure.client.dto.HoldSeatsRequest;
import com.mv.bookingservice.infrastructure.client.dto.SeatHoldResponse;
import com.mv.bookingservice.infrastructure.resilience.ResilienceConfig;
import com.mv.bookingservice.infrastructure.resilience.ResilientCall;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Feign calls to showtime-service, all behind the "showtime" circuit breaker.
 * <ul>
 *     <li>Technical failure (down, timeout, 5xx): counted by the breaker, rethrown as is.</li>
 *     <li>Business answer (4xx): mapped to a booking exception, NOT counted as a failure.</li>
 *     <li>Breaker OPEN: {@code DependencyUnavailableException} (HTTP 503) without calling showtime.</li>
 * </ul>
 */
@Component
public class ShowtimeClientAdapter implements ShowtimeClientPort {

    private static final String SERVICE = "showtime-service";

    private final ShowtimeFeignClient showtimeFeignClient;
    private final CircuitBreaker circuitBreaker;

    public ShowtimeClientAdapter(ShowtimeFeignClient showtimeFeignClient, CircuitBreakerRegistry registry) {
        this.showtimeFeignClient = showtimeFeignClient;
        this.circuitBreaker = registry.circuitBreaker(ResilienceConfig.SHOWTIME);
    }

    @Override
    public SeatHoldResult holdSeats(UUID showtimeId, UUID bookingId, List<UUID> showtimeSeatIds) {
        HoldSeatsRequest request = HoldSeatsRequest.builder()
                .bookingId(bookingId)
                .showtimeSeatIds(showtimeSeatIds)
                .build();

        return ResilientCall.run(circuitBreaker, SERVICE, () -> {
            ApiResponse<SeatHoldResponse> response;
            try {
                response = showtimeFeignClient.holdSeats(showtimeId, request);
            } catch (FeignException e) {
                if (e.status() == 409) {
                    throw new BookingConflictException("Selected seats are no longer available");
                }
                if (e.status() == 404 || e.status() == 400) {
                    throw new BookingValidationException("Showtime or seats not found / not bookable");
                }
                throw e;
            }
            return toSeatHoldResult(requireData(response, "hold seats"));
        });
    }

    @Override
    public SeatHoldResult getHoldByBookingId(UUID bookingId) {
        return ResilientCall.run(circuitBreaker, SERVICE, () ->
                toSeatHoldResult(requireData(showtimeFeignClient.getHoldByBookingId(bookingId), "get hold by booking")));
    }

    @Override
    public SeatHoldResult releaseHold(UUID holdId) {
        return ResilientCall.run(circuitBreaker, SERVICE, () ->
                toSeatHoldResult(requireData(showtimeFeignClient.releaseHold(holdId), "release hold")));
    }

    @Override
    public SeatHoldResult confirmHold(UUID holdId) {
        return ResilientCall.run(circuitBreaker, SERVICE, () -> {
            ApiResponse<SeatHoldResponse> response;
            try {
                response = showtimeFeignClient.confirmHold(holdId);
            } catch (FeignException e) {
                if (e.status() == 409 || e.status() == 404 || e.status() == 410) {
                    throw new SeatHoldRejectedException(
                            "Showtime refused to confirm hold " + holdId + " (HTTP " + e.status() + ")", e);
                }
                throw e; // timeout / 5xx: temporary, the caller retries
            }
            SeatHoldResponse data = requireData(response, "confirm hold");
            // Never trust a 200 alone: only a CONFIRMED hold means the seats are BOOKED.
            if (!"CONFIRMED".equalsIgnoreCase(data.getStatus())) {
                throw new SeatHoldRejectedException(
                        "Showtime answered hold " + holdId + " with status " + data.getStatus() + " instead of CONFIRMED");
            }
            return toSeatHoldResult(data);
        });
    }

    private SeatHoldResult toSeatHoldResult(SeatHoldResponse response) {
        List<HeldSeatDetail> seats = response.getSeats() == null
                ? List.of()
                : response.getSeats().stream()
                .map(this::toHeldSeatDetail)
                .collect(Collectors.toList());

        return new SeatHoldResult(
                response.getHoldId(),
                response.getExpiresAt(),
                response.getMovieTitle(),
                response.getCinemaName(),
                response.getScreenName(),
                response.getStartTime(),
                seats
        );
    }

    private HeldSeatDetail toHeldSeatDetail(HeldSeatResponse response) {
        return new HeldSeatDetail(
                response.getShowtimeSeatId(),
                response.getSeatId(),
                response.getSeatLabel(),
                response.getPrice()
        );
    }

    private SeatHoldResponse requireData(ApiResponse<SeatHoldResponse> response, String action) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            String message = response != null ? response.getMessage() : "empty response";
            throw new IllegalStateException("Showtime service failed to " + action + ": " + message);
        }
        return response.getData();
    }
}
