package com.mv.bookingservice.infrastructure.adapter;

import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.infrastructure.client.ShowtimeFeignClient;
import com.mv.bookingservice.infrastructure.client.dto.ApiResponse;
import com.mv.bookingservice.infrastructure.client.dto.HeldSeatResponse;
import com.mv.bookingservice.infrastructure.client.dto.HoldSeatsRequest;
import com.mv.bookingservice.infrastructure.client.dto.SeatHoldResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ShowtimeClientAdapter implements ShowtimeClientPort {

    private final ShowtimeFeignClient showtimeFeignClient;

    @Override
    public SeatHoldResult holdSeats(UUID showtimeId, UUID bookingId, List<UUID> showtimeSeatIds, String idempotencyKey) {
        HoldSeatsRequest request = HoldSeatsRequest.builder()
                .bookingId(bookingId)
                .showtimeSeatIds(showtimeSeatIds)
                .build();

        ApiResponse<SeatHoldResponse> response = showtimeFeignClient.holdSeats(showtimeId, idempotencyKey, request);
        return toSeatHoldResult(requireData(response, "hold seats"));
    }

    @Override
    public SeatHoldResult getHoldByBookingId(UUID bookingId) {
        ApiResponse<SeatHoldResponse> response = showtimeFeignClient.getHoldByBookingId(bookingId);
        return toSeatHoldResult(requireData(response, "get hold by booking"));
    }

    @Override
    public SeatHoldResult releaseHold(UUID holdId) {
        ApiResponse<SeatHoldResponse> response = showtimeFeignClient.releaseHold(holdId);
        return toSeatHoldResult(requireData(response, "release hold"));
    }

    @Override
    public SeatHoldResult confirmHold(UUID holdId) {
        ApiResponse<SeatHoldResponse> response = showtimeFeignClient.confirmHold(holdId);
        return toSeatHoldResult(requireData(response, "confirm hold"));
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
