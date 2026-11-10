package com.mv.showtimeservice.infrastructure.adapter;

import com.mv.showtimeservice.application.port.out.MovieClientPort;
import com.mv.showtimeservice.infrastructure.client.MovieFeignClient;
import com.mv.showtimeservice.infrastructure.client.dto.RemoteResponse;
import com.mv.showtimeservice.infrastructure.client.dto.MovieDto;
import lombok.RequiredArgsConstructor;
import feign.FeignException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MovieClientAdapter implements MovieClientPort {

    private final MovieFeignClient movieFeignClient;

    @Override
    public MovieSnapshot getMovie(UUID movieId) {
        RemoteResponse<MovieDto> response;
        try {
            response = movieFeignClient.getMovie(movieId);
        } catch (FeignException e) {
            throw RemoteCalls.translate(e, "phim", "Không tìm thấy phim " + movieId);
        } catch (RuntimeException e) {
            throw RemoteCalls.unavailable("phim", e);
        }
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalArgumentException("Phim " + movieId + " không tải được");
        }
        MovieDto movie = response.getData();
        return new MovieSnapshot(
                movie.getId(),
                movie.getTitle(),
                movie.getDurationMinutes(),
                movie.getAgeRating(),
                movie.getStatus()
        );
    }
}
