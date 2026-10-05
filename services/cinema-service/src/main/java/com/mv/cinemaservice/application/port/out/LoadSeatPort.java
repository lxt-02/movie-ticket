package com.mv.cinemaservice.application.port.out;

import com.mv.cinemaservice.domain.model.screen.entity.Seat;

import java.util.List;
import java.util.UUID;

public interface LoadSeatPort {
    List<Seat> findByScreenId(UUID screenId);
}
