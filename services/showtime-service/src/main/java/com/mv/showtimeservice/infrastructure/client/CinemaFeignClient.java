package com.mv.showtimeservice.infrastructure.client;

import com.mv.showtimeservice.infrastructure.client.dto.CinemaDto;
import com.mv.showtimeservice.infrastructure.client.dto.RemoteResponse;
import com.mv.showtimeservice.infrastructure.client.dto.ScreenLayoutDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/** Feign client of cinema-service (endpoints of cinema's InternalCinemaController), used when a showtime is created. */
@FeignClient(name = "cinema-service", url = "${clients.cinema.base-url:}")
public interface CinemaFeignClient {

    @GetMapping("/internal/cinemas/{id}")
    RemoteResponse<CinemaDto> getCinema(@PathVariable("id") UUID id);

    /** Screen and all its physical seats (with the price multiplier of each seat type). */
    @GetMapping("/internal/screens/{id}/layout")
    RemoteResponse<ScreenLayoutDto> getScreenLayout(@PathVariable("id") UUID id);
}
