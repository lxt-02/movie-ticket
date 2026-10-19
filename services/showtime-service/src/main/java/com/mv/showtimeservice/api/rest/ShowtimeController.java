package com.mv.showtimeservice.api.rest;

import com.mv.showtimeservice.api.dto.request.CreateShowtimeRequest;
import com.mv.showtimeservice.api.dto.request.UpdateShowtimeRequest;
import com.mv.showtimeservice.api.dto.response.ApiResponse;
import com.mv.showtimeservice.api.dto.response.PageResponse;
import com.mv.showtimeservice.api.dto.response.ShowtimeResponse;
import com.mv.showtimeservice.api.dto.response.ShowtimeSeatResponse;
import com.mv.showtimeservice.api.security.AdminAccess;
import com.mv.showtimeservice.application.command.CreateShowtimeCommand;
import com.mv.showtimeservice.application.command.UpdateShowtimeCommand;
import com.mv.showtimeservice.application.port.in.CreateShowtimeUseCase;
import com.mv.showtimeservice.application.port.in.GetShowtimeUseCase;
import com.mv.showtimeservice.application.port.in.ManageShowtimeUseCase;
import com.mv.showtimeservice.application.query.SearchShowtimesQuery;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Showtimes: public schedule and seat map (GET), admin create / update / cancel (header X-User-Role: ADMIN).
 */
@RestController
@RequestMapping("/api/v1/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    /** Schedule dates ("date=2026-10-20") are calendar days in Vietnam. */
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private final CreateShowtimeUseCase createShowtimeUseCase;
    private final GetShowtimeUseCase getShowtimeUseCase;
    private final ManageShowtimeUseCase manageShowtimeUseCase;
    private final Clock clock;

    /**
     * Filters (all optional): movieId, cinemaId, screenId, date (Vietnam day) or from/to (ISO instants).
     * Buyers only get upcoming SCHEDULED / SELLING showtimes; admins may also ask for other statuses and the past.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ShowtimeResponse>>> search(
            @RequestParam(required = false) UUID movieId,
            @RequestParam(required = false) UUID cinemaId,
            @RequestParam(required = false) UUID screenId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) List<ShowtimeStatus> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = AdminAccess.ROLE_HEADER, required = false) String role
    ) {
        boolean admin = AdminAccess.isAdmin(role);
        Instant now = clock.instant();
        if (date != null) {
            from = date.atStartOfDay(VIETNAM).toInstant();
            to = date.plusDays(1).atStartOfDay(VIETNAM).toInstant();
        }
        if (!admin && (from == null || from.isBefore(now))) {
            from = now;
        }
        Set<ShowtimeStatus> statuses = admin && status != null && !status.isEmpty()
                ? EnumSet.copyOf(status)
                : null;

        var result = getShowtimeUseCase.search(SearchShowtimesQuery.builder()
                .movieId(movieId)
                .cinemaId(cinemaId)
                .screenId(screenId)
                .from(from)
                .to(to)
                .statuses(statuses)
                .page(page)
                .size(size)
                .build());
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result, ShowtimeController::toResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toResponse(getShowtimeUseCase.getById(id))));
    }

    /** Seat map for the booking screen: showtimeSeatId, label, type, price, status. */
    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<List<ShowtimeSeatResponse>>> getSeats(@PathVariable UUID id) {
        Instant now = clock.instant();
        List<ShowtimeSeatResponse> seats = getShowtimeUseCase.getSeats(id).stream()
                .map(seat -> toSeatResponse(seat, now))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(seats));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShowtimeResponse>> createShowtime(
            @RequestHeader(value = AdminAccess.ROLE_HEADER, required = false) String role,
            @Valid @RequestBody CreateShowtimeRequest request
    ) {
        AdminAccess.require(role);
        Showtime showtime = createShowtimeUseCase.execute(CreateShowtimeCommand.builder()
                .movieId(request.getMovieId())
                .cinemaId(request.getCinemaId())
                .screenId(request.getScreenId())
                .startTime(request.getStartTime())
                .basePrice(request.getBasePrice())
                .build());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Showtime created successfully", toResponse(showtime)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> update(
            @PathVariable UUID id,
            @RequestHeader(value = AdminAccess.ROLE_HEADER, required = false) String role,
            @Valid @RequestBody UpdateShowtimeRequest request
    ) {
        AdminAccess.require(role);
        Showtime showtime = manageShowtimeUseCase.update(id, UpdateShowtimeCommand.builder()
                .startTime(request.getStartTime())
                .basePrice(request.getBasePrice())
                .build());
        return ResponseEntity.ok(ApiResponse.success("Showtime updated", toResponse(showtime)));
    }

    /** Soft delete: CANCELLED and every seat BLOCKED. 409 while seats are held or sold. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> cancel(
            @PathVariable UUID id,
            @RequestHeader(value = AdminAccess.ROLE_HEADER, required = false) String role
    ) {
        AdminAccess.require(role);
        Showtime showtime = manageShowtimeUseCase.cancel(id);
        return ResponseEntity.ok(ApiResponse.success("Showtime cancelled", toResponse(showtime)));
    }

    private static ShowtimeResponse toResponse(Showtime showtime) {
        return ShowtimeResponse.builder()
                .id(showtime.getId())
                .movieId(showtime.getMovieId())
                .screenId(showtime.getScreenId())
                .movieTitle(showtime.getMovieTitle())
                .cinemaId(showtime.getCinemaId())
                .cinemaName(showtime.getCinemaName())
                .screenName(showtime.getScreenName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .basePrice(showtime.getBasePrice())
                .status(showtime.getStatus())
                .createdAt(showtime.getCreatedAt())
                .build();
    }

    private static ShowtimeSeatResponse toSeatResponse(ShowtimeSeat seat, Instant now) {
        return new ShowtimeSeatResponse(seat.getId(), seat.getSeatId(), seat.getSeatLabel(), seat.getSeatType(),
                seat.getPrice(), seat.displayStatus(now));
    }
}
