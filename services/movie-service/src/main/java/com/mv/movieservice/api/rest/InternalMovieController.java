package com.mv.movieservice.api.rest;

import com.mv.movieservice.api.dto.response.ApiResponse;
import com.mv.movieservice.api.dto.response.MovieResponse;
import com.mv.movieservice.api.mapper.MovieDtoMapper;
import com.mv.movieservice.application.port.in.GetMovieUseCase;
import com.mv.movieservice.application.query.GetMovieQuery;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/movies")
@RequiredArgsConstructor
public class InternalMovieController {

    private final GetMovieUseCase getMovieUseCase;
    private final MovieDtoMapper movieDtoMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovieById(@PathVariable UUID id) {
        Movie movie = getMovieUseCase.getById(new GetMovieQuery(id));
        return ResponseEntity.ok(ApiResponse.success(movieDtoMapper.toResponse(movie)));
    }
}
