package com.mv.cinemaservice.api.rest;

import com.mv.cinemaservice.api.dto.request.CinemaRequest;
import com.mv.cinemaservice.api.dto.request.StatusRequest;
import com.mv.cinemaservice.api.dto.response.ApiResponse;
import com.mv.cinemaservice.api.dto.response.CinemaResponse;
import com.mv.cinemaservice.api.dto.response.PageResponse;
import com.mv.cinemaservice.api.dto.response.ScreenResponse;
import com.mv.cinemaservice.api.mapper.CinemaDtoMapper;
import com.mv.cinemaservice.api.security.AdminAccess;
import com.mv.cinemaservice.application.command.SaveCinemaCommand;
import com.mv.cinemaservice.application.port.in.GetCinemaUseCase;
import com.mv.cinemaservice.application.port.in.ManageCinemaUseCase;
import com.mv.cinemaservice.application.query.GetCinemaQuery;
import com.mv.cinemaservice.application.query.SearchCinemasQuery;
import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;
import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;
import com.mv.cinemaservice.domain.model.cinema.exception.CinemaNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Public cinema list / detail / screens (GET) and admin management (header X-User-Role: ADMIN).
 * Closed cinemas are hidden from non-admins.
 */
@RestController
@RequestMapping("/api/v1/cinemas")
@RequiredArgsConstructor
public class CinemaController {

    private final GetCinemaUseCase getCinemaUseCase;
    private final ManageCinemaUseCase manageCinemaUseCase;
    private final CinemaDtoMapper cinemaDtoMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CinemaResponse>>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) CinemaStatus status,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        boolean admin = AdminAccess.isAdmin();
        CinemaStatus effectiveStatus = !admin && status == CinemaStatus.CLOSED ? null : status;
        var result = getCinemaUseCase.search(SearchCinemasQuery.builder()
                .city(city)
                .status(effectiveStatus)
                .keyword(keyword)
                .includeClosed(false)
                .page(page)
                .size(size)
                .build());
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result, (Cinema c) -> cinemaDtoMapper.toResponse(c))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CinemaResponse>> getById(
            @PathVariable UUID id
    ) {
        Cinema cinema = visible(id);
        return ResponseEntity.ok(ApiResponse.success(cinemaDtoMapper.toResponse(cinema)));
    }

    @GetMapping("/{id}/screens")
    public ResponseEntity<ApiResponse<List<ScreenResponse>>> getScreens(
            @PathVariable UUID id
    ) {
        visible(id);
        List<ScreenResponse> screens = getCinemaUseCase.getScreens(id).stream()
                .map((com.mv.cinemaservice.domain.model.screen.aggregate.Screen s) -> cinemaDtoMapper.toResponse(s))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(screens));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CinemaResponse>> create(
            @Valid @RequestBody CinemaRequest request
    ) {
        AdminAccess.require();
        Cinema cinema = manageCinemaUseCase.create(toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cinema created", cinemaDtoMapper.toResponse(cinema)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CinemaResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CinemaRequest request
    ) {
        AdminAccess.require();
        Cinema cinema = manageCinemaUseCase.update(id, toCommand(request));
        return ResponseEntity.ok(ApiResponse.success("Cinema updated", cinemaDtoMapper.toResponse(cinema)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CinemaResponse>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusRequest request
    ) {
        AdminAccess.require();
        Cinema cinema = manageCinemaUseCase.changeStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Cinema status changed", cinemaDtoMapper.toResponse(cinema)));
    }

    /** Soft delete: the cinema becomes CLOSED, existing showtimes / bookings are untouched. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<CinemaResponse>> close(
            @PathVariable UUID id
    ) {
        AdminAccess.require();
        Cinema cinema = manageCinemaUseCase.close(id);
        return ResponseEntity.ok(ApiResponse.success("Cinema closed", cinemaDtoMapper.toResponse(cinema)));
    }

    private Cinema visible(UUID id) {
        Cinema cinema = getCinemaUseCase.getById(new GetCinemaQuery(id));
        if (cinema.isClosed() && !AdminAccess.isAdmin()) {
            throw new CinemaNotFoundException(id);
        }
        return cinema;
    }

    private static SaveCinemaCommand toCommand(CinemaRequest r) {
        return SaveCinemaCommand.builder()
                .name(r.getName())
                .brand(r.getBrand())
                .address(r.getAddress())
                .city(r.getCity())
                .district(r.getDistrict())
                .phone(r.getPhone())
                .status(r.getStatus())
                .build();
    }
}
