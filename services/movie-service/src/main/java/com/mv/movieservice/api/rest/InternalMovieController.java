package com.mv.movieservice.api.rest;

import com.mv.movieservice.api.dto.response.ApiResponse;
import com.mv.movieservice.api.dto.response.MovieDto;
import com.mv.movieservice.api.dto.response.MovieResponse;
import com.mv.movieservice.api.mapper.MovieDtoMapper;
import com.mv.movieservice.application.port.in.GetMovieUseCase;
import com.mv.movieservice.application.query.GetMovieQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Internal movie API (service-to-service), called by showtime-service (its own MovieFeignClient). */
@RestController
@RequiredArgsConstructor
public class InternalMovieController {

    private final GetMovieUseCase getMovieUseCase;
    private final MovieDtoMapper movieDtoMapper;

    @GetMapping("/internal/movies/{id}")
    public ApiResponse<MovieDto> getMovie(@PathVariable("id") UUID id) {
        MovieResponse m = movieDtoMapper.toResponse(getMovieUseCase.getById(new GetMovieQuery(id)));
        return ApiResponse.success(MovieDto.builder()
                .id(m.getId()).title(m.getTitle()).originalTitle(m.getOriginalTitle()).description(m.getDescription())
                .durationMinutes(m.getDurationMinutes()).releaseDate(m.getReleaseDate()).ageRating(m.getAgeRating())
                .language(m.getLanguage()).country(m.getCountry()).posterUrl(m.getPosterUrl())
                .status(m.getStatus() != null ? m.getStatus().name() : null)
                .createdAt(m.getCreatedAt()).updatedAt(m.getUpdatedAt())
                .build());
    }
}
