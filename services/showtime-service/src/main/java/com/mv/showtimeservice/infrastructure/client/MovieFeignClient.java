package com.mv.showtimeservice.infrastructure.client;

import com.mv.showtimeservice.infrastructure.client.dto.ApiResponse;
import com.mv.showtimeservice.infrastructure.client.dto.MovieResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "movie-service", url = "${clients.movie.base-url:http://localhost:8085}")
public interface MovieFeignClient {

    @GetMapping("/internal/movies/{id}")
    ApiResponse<MovieResponse> getMovie(@PathVariable("id") UUID id);
}
