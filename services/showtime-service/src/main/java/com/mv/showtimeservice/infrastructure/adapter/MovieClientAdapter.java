package com.mv.showtimeservice.infrastructure.adapter;

import com.mv.showtimeservice.application.port.out.MovieClientPort;
import com.mv.showtimeservice.infrastructure.client.MovieFeignClient;
import com.mv.showtimeservice.infrastructure.client.dto.ApiResponse;
import com.mv.showtimeservice.infrastructure.client.dto.MovieResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MovieClientAdapter implements MovieClientPort {

    private final MovieFeignClient movieFeignClient;

    @Override
    public MovieSnapshot getMovie(UUID movieId) {
        ApiResponse<MovieResponse> response = movieFeignClient.getMovie(movieId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalArgumentException("Movie " + movieId + " could not be loaded");
        }
        MovieResponse movie = response.getData();
        return new MovieSnapshot(
                movie.getId(),
                movie.getTitle(),
                movie.getDurationMinutes(),
                movie.getAgeRating(),
                movie.getStatus()
        );
    }
}
