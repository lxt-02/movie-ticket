package com.mv.cinemaservice.api.rest;

import com.mv.cinemaservice.api.dto.request.LayoutRequest;
import com.mv.cinemaservice.api.dto.request.ScreenRequest;
import com.mv.cinemaservice.api.dto.request.ScreenStatusRequest;
import com.mv.cinemaservice.api.dto.request.SeatRowRequest;
import com.mv.cinemaservice.api.dto.request.SeatStatusRequest;
import com.mv.cinemaservice.api.dto.response.ApiResponse;
import com.mv.cinemaservice.api.dto.response.ScreenLayoutResponse;
import com.mv.cinemaservice.api.dto.response.ScreenResponse;
import com.mv.cinemaservice.api.dto.response.ScreenTypeResponse;
import com.mv.cinemaservice.api.dto.response.SeatResponse;
import com.mv.cinemaservice.api.dto.response.SeatTypeResponse;
import com.mv.cinemaservice.api.mapper.CinemaDtoMapper;
import com.mv.cinemaservice.api.security.AdminAccess;
import com.mv.cinemaservice.application.command.SaveScreenCommand;
import com.mv.cinemaservice.application.port.in.GetReferenceDataUseCase;
import com.mv.cinemaservice.application.port.in.GetScreenUseCase;
import com.mv.cinemaservice.application.port.in.ManageScreenUseCase;
import com.mv.cinemaservice.application.port.in.ScreenLayoutResult;
import com.mv.cinemaservice.application.query.GetScreenLayoutQuery;
import com.mv.cinemaservice.application.query.GetScreenQuery;
import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;
import com.mv.cinemaservice.domain.model.screen.entity.Seat;
import com.mv.cinemaservice.domain.model.screen.enums.ScreenStatus;
import com.mv.cinemaservice.domain.model.screen.valueobject.SeatRow;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Screens and seat layouts. GET is public; writes need X-User-Role: ADMIN.
 * Layout input is a list of rows: [{ "rowLabel": "A", "seatCount": 12, "seatTypeId": "..." }, ...].
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScreenController {

    private final GetScreenUseCase getScreenUseCase;
    private final ManageScreenUseCase manageScreenUseCase;
    private final GetReferenceDataUseCase referenceDataUseCase;
    private final CinemaDtoMapper mapper;

    @GetMapping("/screen-types")
    public ResponseEntity<ApiResponse<List<ScreenTypeResponse>>> screenTypes() {
        return ResponseEntity.ok(ApiResponse.success(referenceDataUseCase.screenTypes().stream()
                .map(t -> new ScreenTypeResponse(t.id(), t.name(), t.description()))
                .toList()));
    }

    @GetMapping("/seat-types")
    public ResponseEntity<ApiResponse<List<SeatTypeResponse>>> seatTypes() {
        return ResponseEntity.ok(ApiResponse.success(referenceDataUseCase.seatTypes().stream()
                .map(t -> new SeatTypeResponse(t.getId(), t.getName(), t.getPriceMultiplier()))
                .toList()));
    }

    @GetMapping("/screens/{id}")
    public ResponseEntity<ApiResponse<ScreenResponse>> getScreen(@PathVariable UUID id) {
        Screen screen = getScreenUseCase.getById(new GetScreenQuery(id));
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(screen)));
    }

    /** Physical seat layout (seat types, broken seats). For a showtime use /api/v1/showtimes/{id}/seats. */
    @GetMapping("/screens/{id}/layout")
    public ResponseEntity<ApiResponse<ScreenLayoutResponse>> getLayout(@PathVariable UUID id) {
        ScreenLayoutResult layout = getScreenUseCase.getLayout(new GetScreenLayoutQuery(id));
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(layout)));
    }

    @PostMapping("/cinemas/{cinemaId}/screens")
    public ResponseEntity<ApiResponse<ScreenLayoutResponse>> create(
            @PathVariable UUID cinemaId,
            @Valid @RequestBody ScreenRequest request
    ) {
        AdminAccess.require();
        ScreenLayoutResult created = manageScreenUseCase.create(cinemaId, SaveScreenCommand.builder()
                .name(request.getName())
                .screenTypeId(request.getScreenTypeId())
                .rows(toRows(request.getRows()))
                .build());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Screen created", mapper.toResponse(created)));
    }

    @PutMapping("/screens/{id}")
    public ResponseEntity<ApiResponse<ScreenResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ScreenRequest request
    ) {
        AdminAccess.require();
        Screen screen = manageScreenUseCase.update(id, SaveScreenCommand.builder()
                .name(request.getName())
                .screenTypeId(request.getScreenTypeId())
                .build());
        return ResponseEntity.ok(ApiResponse.success("Screen updated", mapper.toResponse(screen)));
    }

    @PatchMapping("/screens/{id}/status")
    public ResponseEntity<ApiResponse<ScreenResponse>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ScreenStatusRequest request
    ) {
        AdminAccess.require();
        Screen screen = manageScreenUseCase.changeStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Screen status changed", mapper.toResponse(screen)));
    }

    /** Replaces the whole seat layout. The screen must be in MAINTENANCE or INACTIVE. */
    @PutMapping("/screens/{id}/layout")
    public ResponseEntity<ApiResponse<ScreenLayoutResponse>> replaceLayout(
            @PathVariable UUID id,
            @Valid @RequestBody LayoutRequest request
    ) {
        AdminAccess.require();
        ScreenLayoutResult layout = manageScreenUseCase.replaceLayout(id, toRows(request.getRows()));
        return ResponseEntity.ok(ApiResponse.success("Seat layout replaced", mapper.toResponse(layout)));
    }

    /** Mark one seat BROKEN / INACTIVE / ACTIVE (applies to showtimes created afterwards). */
    @PatchMapping("/screens/{id}/seats/{seatId}/status")
    public ResponseEntity<ApiResponse<SeatResponse>> changeSeatStatus(
            @PathVariable UUID id,
            @PathVariable UUID seatId,
            @Valid @RequestBody SeatStatusRequest request
    ) {
        AdminAccess.require();
        Seat seat = manageScreenUseCase.changeSeatStatus(id, seatId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Seat status changed", mapper.toResponse(seat)));
    }

    /** Soft delete: the screen becomes INACTIVE (no new showtimes). */
    @DeleteMapping("/screens/{id}")
    public ResponseEntity<ApiResponse<ScreenResponse>> deactivate(
            @PathVariable UUID id
    ) {
        AdminAccess.require();
        Screen screen = manageScreenUseCase.changeStatus(id, ScreenStatus.INACTIVE);
        return ResponseEntity.ok(ApiResponse.success("Screen deactivated", mapper.toResponse(screen)));
    }

    private static List<SeatRow> toRows(List<SeatRowRequest> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .map(r -> new SeatRow(r.getRowLabel(), r.getSeatCount(), r.getSeatTypeId()))
                .toList();
    }
}
