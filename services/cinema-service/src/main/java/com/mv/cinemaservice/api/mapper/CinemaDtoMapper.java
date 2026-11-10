package com.mv.cinemaservice.api.mapper;

import com.mv.cinemaservice.api.dto.response.CinemaResponse;
import com.mv.cinemaservice.api.dto.response.ScreenLayoutResponse;
import com.mv.cinemaservice.api.dto.response.ScreenResponse;
import com.mv.cinemaservice.api.dto.response.SeatResponse;
import com.mv.cinemaservice.application.port.in.ScreenLayoutResult;
import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;
import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;
import com.mv.cinemaservice.domain.model.screen.entity.Seat;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CinemaDtoMapper {

    public CinemaResponse toResponse(Cinema cinema) {
        if (cinema == null) {
            return null;
        }
        return CinemaResponse.builder()
                .id(cinema.getId())
                .name(cinema.getName())
                .brand(cinema.getBrand())
                .address(cinema.getAddress())
                .city(cinema.getCity())
                .district(cinema.getDistrict())
                .phone(cinema.getPhone())
                .status(cinema.getStatus())
                .createdAt(cinema.getCreatedAt())
                .build();
    }

    public ScreenResponse toResponse(Screen screen) {
        if (screen == null) {
            return null;
        }
        return ScreenResponse.builder()
                .id(screen.getId())
                .cinemaId(screen.getCinemaId())
                .screenTypeId(screen.getScreenTypeId())
                .screenTypeName(screen.getScreenTypeName())
                .name(screen.getName())
                .totalSeats(screen.getTotalSeats())
                .status(screen.getStatus())
                .build();
    }

    public SeatResponse toResponse(Seat seat) {
        if (seat == null) {
            return null;
        }
        return SeatResponse.builder()
                .id(seat.getId())
                .screenId(seat.getScreenId())
                .seatTypeId(seat.getSeatTypeId())
                .seatTypeName(seat.getSeatTypeName())
                .priceMultiplier(seat.getPriceMultiplier())
                .rowLabel(seat.getRowLabel())
                .seatNumber(seat.getSeatNumber())
                .label(seat.getLabel())
                .status(seat.getStatus())
                .build();
    }

    public ScreenLayoutResponse toResponse(ScreenLayoutResult layout) {
        if (layout == null) {
            return null;
        }
        List<SeatResponse> seatResponses = new ArrayList<>();
        if (layout.getSeats() != null) {
            seatResponses = layout.getSeats().stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
        return ScreenLayoutResponse.builder()
                .screen(toResponse(layout.getScreen()))
                .seats(seatResponses)
                .build();
    }
}
