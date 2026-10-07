package com.mv.showtimeservice.infrastructure.adapter;

import com.mv.showtimeservice.application.port.out.CinemaClientPort;
import com.mv.showtimeservice.infrastructure.client.CinemaFeignClient;
import com.mv.showtimeservice.infrastructure.client.dto.ApiResponse;
import com.mv.showtimeservice.infrastructure.client.dto.CinemaResponse;
import com.mv.showtimeservice.infrastructure.client.dto.ScreenLayoutResponse;
import com.mv.showtimeservice.infrastructure.client.dto.ScreenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CinemaClientAdapter implements CinemaClientPort {

    private final CinemaFeignClient cinemaFeignClient;

    @Override
    public CinemaSnapshot getCinema(UUID cinemaId) {
        ApiResponse<CinemaResponse> response = cinemaFeignClient.getCinema(cinemaId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalArgumentException("Cinema " + cinemaId + " could not be loaded");
        }
        CinemaResponse cinema = response.getData();
        return new CinemaSnapshot(cinema.getId(), cinema.getName(), cinema.getStatus());
    }

    @Override
    public ScreenLayoutSnapshot getScreenLayout(UUID screenId) {
        ApiResponse<ScreenLayoutResponse> response = cinemaFeignClient.getScreenLayout(screenId);
        if (response == null || !response.isSuccess() || response.getData() == null || response.getData().getScreen() == null) {
            throw new IllegalArgumentException("Screen layout " + screenId + " could not be loaded");
        }
        ScreenLayoutResponse layout = response.getData();
        ScreenResponse screen = layout.getScreen();
        List<SeatSnapshot> seats = layout.getSeats() == null ? List.of() : layout.getSeats().stream()
                .map(seat -> new SeatSnapshot(
                        seat.getId(),
                        seat.getSeatTypeName(),
                        seat.getPriceMultiplier(),
                        seat.getLabel(),
                        seat.getStatus()
                ))
                .toList();
        return new ScreenLayoutSnapshot(
                screen.getId(),
                screen.getCinemaId(),
                screen.getName(),
                screen.getScreenTypeName(),
                screen.getTotalSeats(),
                screen.getStatus(),
                seats
        );
    }
}
