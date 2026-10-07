package com.mv.showtimeservice.infrastructure.client;

import com.mv.showtimeservice.infrastructure.client.dto.ApiResponse;
import com.mv.showtimeservice.infrastructure.client.dto.CinemaResponse;
import com.mv.showtimeservice.infrastructure.client.dto.ScreenLayoutResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "cinema-service", url = "${clients.cinema.base-url:http://localhost:8084}")
public interface CinemaFeignClient {

    @GetMapping("/internal/cinemas/{id}")
    ApiResponse<CinemaResponse> getCinema(@PathVariable("id") UUID id);

    @GetMapping("/internal/screens/{id}/layout")
    ApiResponse<ScreenLayoutResponse> getScreenLayout(@PathVariable("id") UUID id);
}
