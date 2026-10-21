package com.mv.movieservice.api.rest;

import com.mv.movieservice.api.dto.request.GenreRequest;
import com.mv.movieservice.api.dto.request.PersonRequest;
import com.mv.movieservice.api.dto.response.ApiResponse;
import com.mv.movieservice.api.dto.response.GenreResponse;
import com.mv.movieservice.api.dto.response.PageResponse;
import com.mv.movieservice.api.dto.response.PersonResponse;
import com.mv.movieservice.api.mapper.MovieDtoMapper;
import com.mv.movieservice.api.security.AdminAccess;
import com.mv.movieservice.application.port.in.ManageCatalogueUseCase;
import com.mv.movieservice.application.service.MovieSearchSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Genres and persons used by movies, plus the admin "resync search" action.
 * GET is public; POST requires X-User-Role: ADMIN (set by the gateway from the Keycloak token).
 */
@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieCatalogueController {

    private final ManageCatalogueUseCase manageCatalogueUseCase;
    private final MovieSearchSyncService movieSearchSyncService;
    private final MovieDtoMapper movieDtoMapper;

    @GetMapping("/genres")
    public ResponseEntity<ApiResponse<List<GenreResponse>>> listGenres() {
        return ResponseEntity.ok(ApiResponse.success(
                manageCatalogueUseCase.listGenres().stream().map(movieDtoMapper::toResponse).toList()));
    }

    @PostMapping("/genres")
    public ResponseEntity<ApiResponse<GenreResponse>> createGenre(@Valid @RequestBody GenreRequest request) {
        AdminAccess.require();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Genre created",
                movieDtoMapper.toResponse(manageCatalogueUseCase.createGenre(request.getName()))));
    }

    @GetMapping("/persons")
    public ResponseEntity<ApiResponse<PageResponse<PersonResponse>>> searchPersons(
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                manageCatalogueUseCase.searchPersons(keyword, page, size), movieDtoMapper::toResponse)));
    }

    @PostMapping("/persons")
    public ResponseEntity<ApiResponse<PersonResponse>> createPerson(@Valid @RequestBody PersonRequest request) {
        AdminAccess.require();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Person created",
                movieDtoMapper.toResponse(manageCatalogueUseCase.createPerson(
                        request.getName(), request.getAvatarUrl(), request.getBiography()))));
    }

    /** Re-sends every movie to search-service (fills a new / recreated Elasticsearch index). */
    @PostMapping("/search-sync")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> resyncSearch() {
        AdminAccess.require();
        int queued = movieSearchSyncService.resyncAll();
        return ResponseEntity.accepted().body(ApiResponse.success(
                "Movies queued for the search index", Map.of("queued", queued)));
    }
}
