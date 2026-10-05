package com.mv.showtimeservice.infrastructure.persistence.mapper;

import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;
import com.mv.showtimeservice.domain.model.seathold.entity.SeatHoldItem;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import com.mv.showtimeservice.infrastructure.persistence.entity.SeatHoldEntity;
import com.mv.showtimeservice.infrastructure.persistence.entity.SeatHoldItemEntity;
import com.mv.showtimeservice.infrastructure.persistence.entity.SeatHoldItemId;
import com.mv.showtimeservice.infrastructure.persistence.entity.ShowtimeEntity;
import com.mv.showtimeservice.infrastructure.persistence.entity.ShowtimeSeatEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ShowtimePersistenceMapper {

    public SeatHold toDomain(SeatHoldEntity entity) {
        if (entity == null) {
            return null;
        }

        List<SeatHoldItem> items = new ArrayList<>();
        if (entity.getItems() != null) {
            items = entity.getItems().stream()
                    .map(this::toItemDomain)
                    .collect(Collectors.toList());
        }

        return SeatHold.builder()
                .id(entity.getId())
                .bookingId(entity.getBookingId())
                .showtimeId(entity.getShowtimeId())
                .idempotencyKey(entity.getIdempotencyKey())
                .requestHash(entity.getRequestHash())
                .status(entity.getStatus())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .items(items)
                .build();
    }

    public SeatHoldEntity toEntity(SeatHold domain) {
        if (domain == null) {
            return null;
        }

        SeatHoldEntity entity = SeatHoldEntity.builder()
                .id(domain.getId())
                .bookingId(domain.getBookingId())
                .showtimeId(domain.getShowtimeId())
                .idempotencyKey(domain.getIdempotencyKey())
                .requestHash(domain.getRequestHash())
                .status(domain.getStatus())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .items(new ArrayList<>())
                .build();

        if (domain.getItems() != null) {
            for (SeatHoldItem itemDomain : domain.getItems()) {
                SeatHoldItemEntity itemEntity = toItemEntity(itemDomain, entity);
                entity.addItem(itemEntity);
            }
        }

        return entity;
    }

    public SeatHoldItem toItemDomain(SeatHoldItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return SeatHoldItem.builder()
                .holdId(entity.getId() != null ? entity.getId().getHoldId() : null)
                .showtimeSeatId(entity.getId() != null ? entity.getId().getShowtimeSeatId() : null)
                .showtimeId(entity.getShowtimeId())
                .unitPrice(entity.getUnitPrice())
                .build();
    }

    public SeatHoldItemEntity toItemEntity(SeatHoldItem domain, SeatHoldEntity parent) {
        if (domain == null) {
            return null;
        }
        return SeatHoldItemEntity.builder()
                .id(new SeatHoldItemId(parent.getId(), domain.getShowtimeSeatId()))
                .seatHold(parent)
                .showtimeId(parent.getShowtimeId())
                .unitPrice(domain.getUnitPrice())
                .build();
    }

    public Showtime toDomain(ShowtimeEntity entity) {
        if (entity == null) {
            return null;
        }
        return Showtime.builder()
                .id(entity.getId())
                .movieId(entity.getMovieId())
                .screenId(entity.getScreenId())
                .movieTitle(entity.getMovieTitle())
                .cinemaId(entity.getCinemaId())
                .cinemaName(entity.getCinemaName())
                .screenName(entity.getScreenName())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .basePrice(entity.getBasePrice())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ShowtimeEntity toEntity(Showtime domain) {
        if (domain == null) {
            return null;
        }
        return ShowtimeEntity.builder()
                .id(domain.getId())
                .movieId(domain.getMovieId())
                .screenId(domain.getScreenId())
                .movieTitle(domain.getMovieTitle())
                .cinemaId(domain.getCinemaId())
                .cinemaName(domain.getCinemaName())
                .screenName(domain.getScreenName())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .basePrice(domain.getBasePrice())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public ShowtimeSeat toDomain(ShowtimeSeatEntity entity) {
        if (entity == null) {
            return null;
        }
        return ShowtimeSeat.builder()
                .id(entity.getId())
                .showtimeId(entity.getShowtimeId())
                .seatId(entity.getSeatId())
                .seatLabel(entity.getSeatLabel())
                .seatType(entity.getSeatType())
                .price(entity.getPrice())
                .status(entity.getStatus())
                .lockedUntil(entity.getLockedUntil())
                .holdId(entity.getHoldId())
                .build();
    }

    public ShowtimeSeatEntity toEntity(ShowtimeSeat domain) {
        if (domain == null) {
            return null;
        }
        return ShowtimeSeatEntity.builder()
                .id(domain.getId())
                .showtimeId(domain.getShowtimeId())
                .seatId(domain.getSeatId())
                .seatLabel(domain.getSeatLabel())
                .seatType(domain.getSeatType())
                .price(domain.getPrice())
                .status(domain.getStatus())
                .lockedUntil(domain.getLockedUntil())
                .holdId(domain.getHoldId())
                .build();
    }
}
