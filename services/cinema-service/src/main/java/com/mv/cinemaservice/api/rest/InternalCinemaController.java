package com.mv.cinemaservice.api.rest;

import com.mv.cinemaservice.api.dto.response.CinemaResponse;
import com.mv.cinemaservice.api.dto.response.ScreenLayoutResponse;
import com.mv.cinemaservice.api.dto.response.ScreenResponse;
import com.mv.cinemaservice.api.dto.response.SeatResponse;
import com.mv.cinemaservice.api.mapper.CinemaDtoMapper;
import com.mv.cinemaservice.application.port.in.GetCinemaUseCase;
import com.mv.cinemaservice.application.port.in.GetScreenUseCase;
import com.mv.cinemaservice.application.query.GetCinemaQuery;
import com.mv.cinemaservice.application.query.GetScreenLayoutQuery;
import com.mv.cinemaservice.application.query.GetScreenQuery;
import com.mv.cinemaservice.api.dto.response.CinemaDto;
import com.mv.cinemaservice.api.dto.response.ScreenDto;
import com.mv.cinemaservice.api.dto.response.ScreenLayoutDto;
import com.mv.cinemaservice.api.dto.response.SeatDto;
import com.mv.cinemaservice.api.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Internal cinema API (service-to-service). {@link #getCinema} and {@link #getScreenLayout} are called by
 * showtime-service (its own CinemaFeignClient). The internal DTOs carry the same fields as the public responses,
 * with statuses as text.
 */
@RestController
@RequiredArgsConstructor
public class InternalCinemaController {

    private final GetCinemaUseCase getCinemaUseCase;
    private final GetScreenUseCase getScreenUseCase;
    private final CinemaDtoMapper cinemaDtoMapper;

    @GetMapping("/internal/cinemas/{id}")
    public ApiResponse<CinemaDto> getCinema(@PathVariable("id") UUID id) {
        CinemaResponse c = cinemaDtoMapper.toResponse(getCinemaUseCase.getById(new GetCinemaQuery(id)));
        return ApiResponse.success(CinemaDto.builder()
                .id(c.getId()).name(c.getName()).brand(c.getBrand()).address(c.getAddress()).city(c.getCity())
                .district(c.getDistrict()).phone(c.getPhone()).status(name(c.getStatus())).createdAt(c.getCreatedAt())
                .build());
    }

    /** Screen details (not used by other services at the moment). */
    @GetMapping("/internal/screens/{id}")
    public ApiResponse<ScreenDto> getScreenById(@PathVariable("id") UUID id) {
        return ApiResponse.success(toDto(cinemaDtoMapper.toResponse(getScreenUseCase.getById(new GetScreenQuery(id)))));
    }

    /** Screen and all its physical seats (with the price multiplier of each seat type). */
    @GetMapping("/internal/screens/{id}/layout")
    public ApiResponse<ScreenLayoutDto> getScreenLayout(@PathVariable("id") UUID id) {
        ScreenLayoutResponse layout = cinemaDtoMapper.toResponse(getScreenUseCase.getLayout(new GetScreenLayoutQuery(id)));
        List<SeatDto> seats = layout.getSeats() == null ? List.of() : layout.getSeats().stream().map(InternalCinemaController::toDto).toList();
        return ApiResponse.success(ScreenLayoutDto.builder().screen(toDto(layout.getScreen())).seats(seats).build());
    }

    private static ScreenDto toDto(ScreenResponse s) {
        if (s == null) {
            return null;
        }
        return ScreenDto.builder()
                .id(s.getId()).cinemaId(s.getCinemaId()).screenTypeId(s.getScreenTypeId())
                .screenTypeName(s.getScreenTypeName()).name(s.getName()).totalSeats(s.getTotalSeats())
                .status(name(s.getStatus()))
                .build();
    }

    private static SeatDto toDto(SeatResponse s) {
        return SeatDto.builder()
                .id(s.getId()).screenId(s.getScreenId()).seatTypeId(s.getSeatTypeId()).seatTypeName(s.getSeatTypeName())
                .priceMultiplier(s.getPriceMultiplier()).rowLabel(s.getRowLabel()).seatNumber(s.getSeatNumber())
                .label(s.getLabel()).status(name(s.getStatus()))
                .build();
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
