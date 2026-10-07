package com.mv.showtimeservice.infrastructure.persistence.repository;

import com.mv.showtimeservice.infrastructure.persistence.entity.ShowtimeSeatEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShowtimeSeatJpaRepository extends JpaRepository<ShowtimeSeatEntity, UUID> {

    List<ShowtimeSeatEntity> findByShowtimeId(UUID showtimeId);

    List<ShowtimeSeatEntity> findByHoldId(UUID holdId);

    boolean existsByShowtimeId(UUID showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ShowtimeSeatEntity s WHERE s.id IN :ids ORDER BY s.id ASC")
    List<ShowtimeSeatEntity> findByIdsWithLock(@Param("ids") List<UUID> ids);
}
