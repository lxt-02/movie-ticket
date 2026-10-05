package com.mv.cinemaservice.infrastructure.persistence.mapper;

import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;
import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;
import com.mv.cinemaservice.domain.model.screen.entity.Seat;
import com.mv.cinemaservice.domain.model.screen.entity.SeatType;
import com.mv.cinemaservice.infrastructure.persistence.entity.CinemaEntity;
import com.mv.cinemaservice.infrastructure.persistence.entity.ScreenEntity;
import com.mv.cinemaservice.infrastructure.persistence.entity.ScreenTypeEntity;
import com.mv.cinemaservice.infrastructure.persistence.entity.SeatEntity;
import com.mv.cinemaservice.infrastructure.persistence.entity.SeatTypeEntity;
import org.springframework.stereotype.Component;

@Component
public class CinemaPersistenceMapper {

    public Cinema toDomain(CinemaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Cinema.builder()
                .id(entity.getId())
                .name(entity.getName())
                .brand(entity.getBrand())
                .address(entity.getAddress())
                .city(entity.getCity())
                .district(entity.getDistrict())
                .phone(entity.getPhone())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public CinemaEntity toEntity(Cinema domain) {
        if (domain == null) {
            return null;
        }
        return CinemaEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .brand(domain.getBrand())
                .address(domain.getAddress())
                .city(domain.getCity())
                .district(domain.getDistrict())
                .phone(domain.getPhone())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public Screen toDomain(ScreenEntity entity) {
        if (entity == null) {
            return null;
        }
        return Screen.builder()
                .id(entity.getId())
                .cinemaId(entity.getCinema() != null ? entity.getCinema().getId() : null)
                .screenTypeId(entity.getScreenType() != null ? entity.getScreenType().getId() : null)
                .screenTypeName(entity.getScreenType() != null ? entity.getScreenType().getName() : null)
                .name(entity.getName())
                .totalSeats(entity.getTotalSeats())
                .status(entity.getStatus())
                .build();
    }

    public ScreenEntity toEntity(Screen domain, CinemaEntity cinema, ScreenTypeEntity screenType) {
        if (domain == null) {
            return null;
        }
        return ScreenEntity.builder()
                .id(domain.getId())
                .cinema(cinema)
                .screenType(screenType)
                .name(domain.getName())
                .totalSeats(domain.getTotalSeats())
                .status(domain.getStatus())
                .build();
    }

    public Seat toDomain(SeatEntity entity) {
        if (entity == null) {
            return null;
        }
        return Seat.builder()
                .id(entity.getId())
                .screenId(entity.getScreen() != null ? entity.getScreen().getId() : null)
                .seatTypeId(entity.getSeatType() != null ? entity.getSeatType().getId() : null)
                .seatTypeName(entity.getSeatType() != null ? entity.getSeatType().getName() : null)
                .priceMultiplier(entity.getSeatType() != null ? entity.getSeatType().getPriceMultiplier() : null)
                .rowLabel(entity.getRowLabel())
                .seatNumber(entity.getSeatNumber())
                .label(entity.getLabel())
                .status(entity.getStatus())
                .build();
    }

    public SeatType toDomain(SeatTypeEntity entity) {
        if (entity == null) {
            return null;
        }
        return SeatType.builder()
                .id(entity.getId())
                .name(entity.getName())
                .priceMultiplier(entity.getPriceMultiplier())
                .build();
    }
}
