package com.mv.showtimeservice.application.port.out;

import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;

import java.util.List;
import java.util.UUID;

public interface ShowtimeSeatRepositoryPort {
    List<ShowtimeSeat> findByIdsWithLock(List<UUID> ids);

    List<ShowtimeSeat> findByHoldId(UUID holdId);

    List<ShowtimeSeat> saveAll(List<ShowtimeSeat> seats);

    boolean existsByShowtimeId(UUID showtimeId);
}
