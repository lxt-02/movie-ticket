package com.mv.showtimeservice.infrastructure.client;

import com.mv.showtimeservice.infrastructure.client.dto.MovieDto;
import com.mv.showtimeservice.infrastructure.client.dto.RemoteResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/** Feign client of movie-service (endpoint of movie's InternalMovieController), used when a showtime is created. */
@FeignClient(name = "movie-service", url = "${clients.movie.base-url:}")
public interface MovieFeignClient {

    @GetMapping("/internal/movies/{id}")
    RemoteResponse<MovieDto> getMovie(@PathVariable("id") UUID id);
}
