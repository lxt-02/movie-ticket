package com.mv.showtimeservice.api.rest;

import com.mv.showtimeservice.api.dto.response.ApiResponse;
import com.mv.showtimeservice.api.dto.request.HoldSeatsRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Seat hold API (service-to-service), called by booking-service (its own ShowtimeFeignClient).
 * Full paths on each method, no class-level mapping.
 */
@RestController
@RequiredArgsConstructor
public class InternalShowtimeController {

    private final HoldSeatsUseCase holdSeatsUseCase;
    private final GetHoldUseCase getHoldUseCase;
    private final ReleaseHoldUseCase releaseHoldUseCase;
    private final ConfirmHoldUseCase confirmHoldUseCase;
    private final ShowtimeDtoMapper showtimeDtoMapper;

    /** Idempotency is keyed by bookingId (unique per hold); no caller-supplied key is needed. */
    @PostMapping("/internal/showtimes/{id}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SeatHoldResponse> holdSeats(@PathVariable("id") UUID showtimeId,
                                                   @Valid @RequestBody HoldSeatsRequest request) {
        HoldSeatsCommand command = showtimeDtoMapper.toCommand(showtimeId, request);
        HoldSeatsResult result = holdSeatsUseCase.execute(command);
        return ApiResponse.success("Giữ ghế thành công", showtimeDtoMapper.toResponse(result));
    }

    @GetMapping("/internal/holds/by-booking/{bookingId}")
    public ApiResponse<SeatHoldResponse> getHoldByBookingId(@PathVariable("bookingId") UUID bookingId) {
        SeatHold hold = getHoldUseCase.getByBookingId(GetHoldByBookingIdQuery.builder().bookingId(bookingId).build());
        return ApiResponse.success("Đã tìm thấy lượt giữ ghế", showtimeDtoMapper.toResponse(hold, null, null));
    }

    /** Not used by booking-service at the moment. */
    @GetMapping("/internal/holds/{id}")
    public ApiResponse<SeatHoldResponse> getHoldById(@PathVariable("id") UUID holdId) {
        SeatHold hold = getHoldUseCase.getById(GetHoldByIdQuery.builder().holdId(holdId).build());
        return ApiResponse.success("Đã tìm thấy lượt giữ ghế", showtimeDtoMapper.toResponse(hold, null, null));
    }

    /** Gives the held seats back (booking cancelled / expired). */
    @PostMapping("/internal/holds/{id}/release")
    public ApiResponse<SeatHoldResponse> releaseHold(@PathVariable("id") UUID holdId) {
        SeatHold hold = releaseHoldUseCase.execute(ReleaseHoldCommand.builder().holdId(holdId).build());
        return ApiResponse.success("Đã nhả ghế", showtimeDtoMapper.toResponse(hold, null, null));
    }

    /** Held seats become BOOKED after payment. 409 if the hold is no longer valid. */
    @PostMapping("/internal/holds/{id}/confirm")
    public ApiResponse<SeatHoldResponse> confirmHold(@PathVariable("id") UUID holdId) {
        SeatHold hold = confirmHoldUseCase.execute(ConfirmHoldCommand.builder().holdId(holdId).build());
        return ApiResponse.success("Đã xác nhận ghế", showtimeDtoMapper.toResponse(hold, null, null));
    }
}
