package com.mv.cinemaservice.api.rest;

import com.mv.cinemaservice.api.dto.response.ApiResponse;
import com.mv.cinemaservice.api.dto.response.CinemaResponse;
import com.mv.cinemaservice.api.dto.response.ScreenLayoutResponse;
import com.mv.cinemaservice.api.dto.response.ScreenResponse;
import com.mv.cinemaservice.api.mapper.CinemaDtoMapper;
import com.mv.cinemaservice.application.port.in.GetCinemaUseCase;
import com.mv.cinemaservice.application.port.in.GetScreenUseCase;
import com.mv.cinemaservice.application.port.in.ScreenLayoutResult;
import com.mv.cinemaservice.application.query.GetCinemaQuery;
import com.mv.cinemaservice.application.query.GetScreenLayoutQuery;
import com.mv.cinemaservice.application.query.GetScreenQuery;
import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;
import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalCinemaController {

    private final GetCinemaUseCase getCinemaUseCase;
    private final GetScreenUseCase getScreenUseCase;
    private final CinemaDtoMapper cinemaDtoMapper;

    /**
     * Internal endpoint called by Showtime Service to verify cinema details.
     * Contract: GET /internal/cinemas/{id}
     */
    @GetMapping("/cinemas/{id}")
    public ResponseEntity<ApiResponse<CinemaResponse>> getCinemaById(@PathVariable("id") UUID id) {
        Cinema cinema = getCinemaUseCase.getById(new GetCinemaQuery(id));
        CinemaResponse response = cinemaDtoMapper.toResponse(cinema);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Internal endpoint called by Showtime Service to verify screen details.
     * Contract: GET /internal/screens/{id}
     */
    @GetMapping("/screens/{id}")
    public ResponseEntity<ApiResponse<ScreenResponse>> getScreenById(@PathVariable("id") UUID id) {
        Screen screen = getScreenUseCase.getById(new GetScreenQuery(id));
        ScreenResponse response = cinemaDtoMapper.toResponse(screen);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Internal endpoint called by Showtime Service when creating a showtime.
     * Fetches screen details and full seat layout with price multipliers.
     * Contract: GET /internal/screens/{id}/layout
     */
    @GetMapping("/screens/{id}/layout")
    public ResponseEntity<ApiResponse<ScreenLayoutResponse>> getScreenLayout(@PathVariable("id") UUID id) {
        ScreenLayoutResult result = getScreenUseCase.getLayout(new GetScreenLayoutQuery(id));
        ScreenLayoutResponse response = cinemaDtoMapper.toResponse(result);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
