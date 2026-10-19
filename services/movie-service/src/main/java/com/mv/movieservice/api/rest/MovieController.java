package com.mv.movieservice.api.rest;

import com.mv.movieservice.api.dto.request.MovieRequest;
import com.mv.movieservice.api.dto.request.StatusRequest;
import com.mv.movieservice.api.dto.response.ApiResponse;
import com.mv.movieservice.api.dto.response.MovieResponse;
import com.mv.movieservice.api.dto.response.PageResponse;
import com.mv.movieservice.api.mapper.MovieDtoMapper;
import com.mv.movieservice.api.security.AdminAccess;
import com.mv.movieservice.application.command.SaveMovieCommand;
import com.mv.movieservice.application.port.in.GetMovieUseCase;
import com.mv.movieservice.application.port.in.ManageMovieUseCase;
import com.mv.movieservice.application.query.GetMovieQuery;
import com.mv.movieservice.application.query.SearchMoviesQuery;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import com.mv.movieservice.domain.model.movie.exception.MovieNotFoundException;
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

import java.util.UUID;

/**
 * Public movie catalogue (GET) and admin management (POST / PUT / PATCH / DELETE, header X-User-Role: ADMIN).
 * Archived movies are hidden from non-admins.
 */
@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final GetMovieUseCase getMovieUseCase;
    private final ManageMovieUseCase manageMovieUseCase;
    private final MovieDtoMapper movieDtoMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MovieResponse>>> search(
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = AdminAccess.ROLE_HEADER, required = false) String role
    ) {
        boolean admin = AdminAccess.isAdmin(role);
        // Non-admins never see archived movies, even when asking for them.
        MovieStatus effectiveStatus = !admin && status == MovieStatus.ARCHIVED ? null : status;
        var result = getMovieUseCase.search(SearchMoviesQuery.builder()
                .status(effectiveStatus)
                .keyword(keyword)
                .includeArchived(false)
                .page(page)
                .size(size)
                .build());
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result, movieDtoMapper::toResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getById(
            @PathVariable UUID id,
            @RequestHeader(value = AdminAccess.ROLE_HEADER, required = false) String role
    ) {
        Movie movie = getMovieUseCase.getById(new GetMovieQuery(id));
        if (movie.isArchived() && !AdminAccess.isAdmin(role)) {
            throw new MovieNotFoundException(id);
        }
        return ResponseEntity.ok(ApiResponse.success(movieDtoMapper.toResponse(movie)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MovieResponse>> create(
            @RequestHeader(value = AdminAccess.ROLE_HEADER, required = false) String role,
            @Valid @RequestBody MovieRequest request
    ) {
        AdminAccess.require(role);
        Movie movie = manageMovieUseCase.create(toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Movie created", movieDtoMapper.toResponse(movie)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> update(
            @PathVariable UUID id,
            @RequestHeader(value = AdminAccess.ROLE_HEADER, required = false) String role,
            @Valid @RequestBody MovieRequest request
    ) {
        AdminAccess.require(role);
        Movie movie = manageMovieUseCase.update(id, toCommand(request));
        return ResponseEntity.ok(ApiResponse.success("Movie updated", movieDtoMapper.toResponse(movie)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MovieResponse>> changeStatus(
            @PathVariable UUID id,
            @RequestHeader(value = AdminAccess.ROLE_HEADER, required = false) String role,
            @Valid @RequestBody StatusRequest request
    ) {
        AdminAccess.require(role);
        Movie movie = manageMovieUseCase.changeStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Movie status changed", movieDtoMapper.toResponse(movie)));
    }

    /** Soft delete: the movie is ARCHIVED, existing showtimes / bookings are untouched. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> archive(
            @PathVariable UUID id,
            @RequestHeader(value = AdminAccess.ROLE_HEADER, required = false) String role
    ) {
        AdminAccess.require(role);
        Movie movie = manageMovieUseCase.archive(id);
        return ResponseEntity.ok(ApiResponse.success("Movie archived", movieDtoMapper.toResponse(movie)));
    }

    private static SaveMovieCommand toCommand(MovieRequest r) {
        return SaveMovieCommand.builder()
                .title(r.getTitle())
                .originalTitle(r.getOriginalTitle())
                .description(r.getDescription())
                .durationMinutes(r.getDurationMinutes())
                .releaseDate(r.getReleaseDate())
                .ageRating(r.getAgeRating())
                .language(r.getLanguage())
                .country(r.getCountry())
                .posterUrl(r.getPosterUrl())
                .status(r.getStatus())
                .build();
    }
}
