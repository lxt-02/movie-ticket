package com.mv.showtimeservice.api.rest;

import com.mv.showtimeservice.api.dto.request.HoldSeatsRequest;
import com.mv.showtimeservice.api.dto.response.ApiResponse;
import com.mv.showtimeservice.api.dto.response.SeatHoldResponse;
import com.mv.showtimeservice.api.mapper.ShowtimeDtoMapper;
import com.mv.showtimeservice.application.command.ConfirmHoldCommand;
import com.mv.showtimeservice.application.command.HoldSeatsCommand;
import com.mv.showtimeservice.application.command.ReleaseHoldCommand;
import com.mv.showtimeservice.application.port.in.ConfirmHoldUseCase;
import com.mv.showtimeservice.application.port.in.GetHoldUseCase;
import com.mv.showtimeservice.application.port.in.HoldSeatsResult;
import com.mv.showtimeservice.application.port.in.HoldSeatsUseCase;
import com.mv.showtimeservice.application.port.in.ReleaseHoldUseCase;
import com.mv.showtimeservice.application.query.GetHoldByBookingIdQuery;
import com.mv.showtimeservice.application.query.GetHoldByIdQuery;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalShowtimeController {

    private final HoldSeatsUseCase holdSeatsUseCase;
    private final GetHoldUseCase getHoldUseCase;
    private final ReleaseHoldUseCase releaseHoldUseCase;
    private final ConfirmHoldUseCase confirmHoldUseCase;
    private final ShowtimeDtoMapper showtimeDtoMapper;

    /**
     * Endpoint called by Booking Service to atomically hold seats.
     * Contract: POST /internal/showtimes/{id}/holds
     */
    @PostMapping("/showtimes/{id}/holds")
    public ResponseEntity<ApiResponse<SeatHoldResponse>> holdSeats(
            @PathVariable("id") UUID showtimeId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody HoldSeatsRequest request
    ) {
        HoldSeatsCommand command = showtimeDtoMapper.toCommand(showtimeId, request, idempotencyKey);
        HoldSeatsResult result = holdSeatsUseCase.execute(command);
        SeatHoldResponse response = showtimeDtoMapper.toResponse(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Seats held successfully", response));
    }

    /**
     * Endpoint called by Booking Service to query hold by booking ID after timeout / retry.
     * Contract: GET /internal/holds/by-booking/{bookingId}
     */
    @GetMapping("/holds/by-booking/{bookingId}")
    public ResponseEntity<ApiResponse<SeatHoldResponse>> getHoldByBookingId(
            @PathVariable("bookingId") UUID bookingId
    ) {
        GetHoldByBookingIdQuery query = GetHoldByBookingIdQuery.builder()
                .bookingId(bookingId)
                .build();
        SeatHold hold = getHoldUseCase.getByBookingId(query);
        SeatHoldResponse response = showtimeDtoMapper.toResponse(hold, null, null);
        return ResponseEntity.ok(ApiResponse.success("Hold found", response));
    }

    /**
     * Endpoint to query hold by hold ID.
     * Contract: GET /internal/holds/{id}
     */
    @GetMapping("/holds/{id}")
    public ResponseEntity<ApiResponse<SeatHoldResponse>> getHoldById(
            @PathVariable("id") UUID holdId
    ) {
        GetHoldByIdQuery query = GetHoldByIdQuery.builder()
                .holdId(holdId)
                .build();
        SeatHold hold = getHoldUseCase.getById(query);
        SeatHoldResponse response = showtimeDtoMapper.toResponse(hold, null, null);
        return ResponseEntity.ok(ApiResponse.success("Hold found", response));
    }

    /**
     * Endpoint to release held seats when booking fails or expires.
     * Contract: POST /internal/holds/{id}/release
     */
    @PostMapping("/holds/{id}/release")
    public ResponseEntity<ApiResponse<SeatHoldResponse>> releaseHold(
            @PathVariable("id") UUID holdId
    ) {
        ReleaseHoldCommand command = ReleaseHoldCommand.builder()
                .holdId(holdId)
                .build();
        SeatHold hold = releaseHoldUseCase.execute(command);
        SeatHoldResponse response = showtimeDtoMapper.toResponse(hold, null, null);
        return ResponseEntity.ok(ApiResponse.success("Hold released successfully", response));
    }

    /**
     * Endpoint to confirm held seats (BOOKED) after successful payment.
     * Contract: POST /internal/holds/{id}/confirm
     */
    @PostMapping("/holds/{id}/confirm")
    public ResponseEntity<ApiResponse<SeatHoldResponse>> confirmHold(
            @PathVariable("id") UUID holdId
    ) {
        ConfirmHoldCommand command = ConfirmHoldCommand.builder()
                .holdId(holdId)
                .build();
        SeatHold hold = confirmHoldUseCase.execute(command);
        SeatHoldResponse response = showtimeDtoMapper.toResponse(hold, null, null);
        return ResponseEntity.ok(ApiResponse.success("Hold confirmed successfully", response));
    }
}
