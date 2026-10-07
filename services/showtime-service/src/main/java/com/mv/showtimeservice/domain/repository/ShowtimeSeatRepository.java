package com.mv.showtimeservice.domain.repository;

import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShowtimeSeatRepository {
    List<ShowtimeSeat> findByShowtimeId(UUID showtimeId);
    List<ShowtimeSeat> findByIdsWithLock(List<UUID> ids);
    List<ShowtimeSeat> findByHoldId(UUID holdId);
    List<ShowtimeSeat> saveAll(List<ShowtimeSeat> seats);
    Optional<ShowtimeSeat> findById(UUID id);
    boolean existsByShowtimeId(UUID showtimeId);
}
