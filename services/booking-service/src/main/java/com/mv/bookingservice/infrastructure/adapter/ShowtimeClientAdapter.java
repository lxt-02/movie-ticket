package com.mv.bookingservice.infrastructure.adapter;

import com.mv.bookingservice.application.exception.SeatHoldRejectedException;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.exception.BookingConflictException;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import com.mv.bookingservice.infrastructure.client.FeignErrors;
import com.mv.bookingservice.infrastructure.client.ShowtimeFeignClient;
import com.mv.bookingservice.infrastructure.client.dto.RemoteResponse;
import com.mv.bookingservice.infrastructure.client.dto.HeldSeatResponse;
import com.mv.bookingservice.infrastructure.client.dto.HoldSeatsRequest;
import com.mv.bookingservice.infrastructure.client.dto.SeatHoldResponse;
import com.mv.bookingservice.infrastructure.resilience.ResilienceConfig;
import com.mv.bookingservice.infrastructure.resilience.RemoteCallGuard;
import feign.FeignException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final RemoteCallGuard guard;

    /** Retry → CircuitBreaker → Bulkhead, all named "showtime". */
    @Autowired
    public ShowtimeClientAdapter(ShowtimeFeignClient showtimeFeignClient, CircuitBreakerRegistry breakers, RetryRegistry retries,
                 BulkheadRegistry bulkheads) {
        this.showtimeFeignClient = showtimeFeignClient;
        this.guard = new RemoteCallGuard(SERVICE, breakers.circuitBreaker(ResilienceConfig.SHOWTIME),
                retries.retry(ResilienceConfig.SHOWTIME), bulkheads.bulkhead(ResilienceConfig.SHOWTIME));
    }

    /** Circuit breaker only (unit tests of the breaker). */
    public ShowtimeClientAdapter(ShowtimeFeignClient showtimeFeignClient, CircuitBreakerRegistry breakers) {
        this.showtimeFeignClient = showtimeFeignClient;
        this.guard = RemoteCallGuard.breakerOnly(SERVICE, breakers.circuitBreaker(ResilienceConfig.SHOWTIME));
    }

    /** Any combination, for tests. */
    public ShowtimeClientAdapter(ShowtimeFeignClient showtimeFeignClient, RemoteCallGuard guard) {
        this.showtimeFeignClient = showtimeFeignClient;
        this.guard = guard;
    }

    @Override
    public SeatHoldResult holdSeats(UUID showtimeId, UUID bookingId, List<UUID> showtimeSeatIds) {
        HoldSeatsRequest request = HoldSeatsRequest.builder()
                .bookingId(bookingId)
                .showtimeSeatIds(showtimeSeatIds)
                .build();

        return guard.call(() -> {
            RemoteResponse<SeatHoldResponse> response;
            try {
                response = showtimeFeignClient.holdSeats(showtimeId, request);
            } catch (FeignException e) {
                // Business answers keep showtime's Vietnamese message ("Ghế A3 vừa có người khác chọn"...).
                if (e.status() == 409) {
                    throw new BookingConflictException(FeignErrors.messageOr(e,
                            "Ghế bạn chọn vừa có người khác giữ, vui lòng chọn ghế khác"));
                }
                if (e.status() == 404 || e.status() == 400) {
                    throw new BookingValidationException(FeignErrors.messageOr(e,
                            "Suất chiếu hoặc ghế không tồn tại / không đặt được"));
                }
                throw e;
            }
            return toSeatHoldResult(requireData(response, "hold seats"));
        });
    }

    @Override
    public SeatHoldResult getHoldByBookingId(UUID bookingId) {
        return guard.call(() ->
                toSeatHoldResult(requireData(showtimeFeignClient.getHoldByBookingId(bookingId), "get hold by booking")));
    }

    @Override
    public SeatHoldResult releaseHold(UUID holdId) {
        return guard.call(() ->
                toSeatHoldResult(requireData(showtimeFeignClient.releaseHold(holdId), "release hold")));
    }

    @Override
    public SeatHoldResult confirmHold(UUID holdId) {
        return guard.call(() -> {
            RemoteResponse<SeatHoldResponse> response;
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

    private SeatHoldResponse requireData(RemoteResponse<SeatHoldResponse> response, String action) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            String message = response != null ? response.getMessage() : "empty response";
            throw new IllegalStateException("Showtime service failed to " + action + ": " + message);
        }
        return response.getData();
    }
}
