package com.mv.searchservice.api;

import com.mv.searchservice.elasticsearch.MovieIndexClient;
import com.mv.searchservice.search.MovieSearchCriteria;
import com.mv.searchservice.search.MovieSearchResult;
import com.mv.searchservice.search.MovieSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Public movie search (no login needed). Archived movies are never in the index.
 * <pre>
 * GET /api/v1/search/movies?q=dune&genres=Action&genres=Drama&ageRating=T13&releaseFrom=2024-01-01&sort=newest
 * GET /api/v1/search/movies/suggest?q=lat ma
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class MovieSearchController {

    static final String ROLE_HEADER = "X-User-Role";

    private final MovieSearchService movieSearchService;
    private final MovieIndexClient movieIndexClient;

    @GetMapping("/movies")
    public ResponseEntity<ApiResponse<MovieSearchResult>> searchMovies(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String person,
            @RequestParam(required = false) List<String> genres,
            @RequestParam(required = false, name = "status") List<String> statuses,
            @RequestParam(required = false, name = "ageRating") List<String> ageRatings,
            @RequestParam(required = false, name = "language") List<String> languages,
            @RequestParam(required = false, name = "country") List<String> countries,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseTo,
            @RequestParam(required = false) Integer durationMin,
            @RequestParam(required = false) Integer durationMax,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        MovieSearchCriteria criteria = new MovieSearchCriteria(q, person, genres, statuses, ageRatings, languages,
                countries, releaseFrom, releaseTo, durationMin, durationMax, MovieSearchCriteria.Sort.parse(sort),
                page, size);
        return ResponseEntity.ok(ApiResponse.success(movieSearchService.search(criteria)));
    }

    @GetMapping("/movies/suggest")
    public ResponseEntity<ApiResponse<List<MovieSearchResult.Suggestion>>> suggest(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(movieSearchService.suggest(q, size)));
    }

    /**
     * ADMIN: drop and recreate the (empty) index, e.g. after changing movies-index.json. Then call
     * POST /api/v1/movies/search-sync on movie-service to refill it.
     */
    @PostMapping("/admin/movies/recreate-index")
    public ResponseEntity<ApiResponse<Map<String, String>>> recreateIndex(
            @RequestHeader(value = ROLE_HEADER, required = false) String role
    ) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new ForbiddenException("Chỉ ADMIN mới được tạo lại index tìm kiếm");
        }
        movieIndexClient.recreateIndex();
        return ResponseEntity.ok(ApiResponse.success(
                "Index recreated, now call POST /api/v1/movies/search-sync to refill it",
                Map.of("index", movieIndexClient.indexName())));
    }
}
