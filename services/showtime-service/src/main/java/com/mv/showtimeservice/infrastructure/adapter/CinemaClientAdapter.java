package com.mv.showtimeservice.infrastructure.adapter;

import com.mv.showtimeservice.application.port.out.CinemaClientPort;
import com.mv.showtimeservice.infrastructure.client.CinemaFeignClient;
import com.mv.showtimeservice.infrastructure.client.dto.CinemaDto;
import com.mv.showtimeservice.infrastructure.client.dto.ScreenDto;
import com.mv.showtimeservice.infrastructure.client.dto.ScreenLayoutDto;
import com.mv.showtimeservice.infrastructure.client.dto.RemoteResponse;
import lombok.RequiredArgsConstructor;
import feign.FeignException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CinemaClientAdapter implements CinemaClientPort {

    private final CinemaFeignClient cinemaFeignClient;

    @Override
    public CinemaSnapshot getCinema(UUID cinemaId) {
        RemoteResponse<CinemaDto> response;
        try {
            response = cinemaFeignClient.getCinema(cinemaId);
        } catch (FeignException e) {
            throw RemoteCalls.translate(e, "rạp", "Không tìm thấy rạp " + cinemaId);
        } catch (RuntimeException e) {
            throw RemoteCalls.unavailable("rạp", e);
        }
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalArgumentException("Rạp " + cinemaId + " không tải được");
        }
        CinemaDto cinema = response.getData();
        return new CinemaSnapshot(cinema.getId(), cinema.getName(), cinema.getStatus());
    }

    @Override
    public ScreenLayoutSnapshot getScreenLayout(UUID screenId) {
        RemoteResponse<ScreenLayoutDto> response;
        try {
            response = cinemaFeignClient.getScreenLayout(screenId);
        } catch (FeignException e) {
            throw RemoteCalls.translate(e, "rạp", "Không tìm thấy phòng chiếu " + screenId);
        } catch (RuntimeException e) {
            throw RemoteCalls.unavailable("rạp", e);
        }
        if (response == null || !response.isSuccess() || response.getData() == null || response.getData().getScreen() == null) {
            throw new IllegalArgumentException("Sơ đồ ghế của phòng " + screenId + " không tải được");
        }
        ScreenLayoutDto layout = response.getData();
        ScreenDto screen = layout.getScreen();
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
