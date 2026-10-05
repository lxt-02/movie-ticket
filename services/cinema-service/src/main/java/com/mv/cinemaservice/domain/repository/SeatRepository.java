package com.mv.cinemaservice.domain.repository;

import com.mv.cinemaservice.domain.model.screen.entity.Seat;

import java.util.List;
import java.util.UUID;

public interface SeatRepository {
    List<Seat> findByScreenId(UUID screenId);
}
