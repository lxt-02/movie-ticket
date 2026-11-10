package com.mv.cinemaservice.infrastructure.persistence.repository;

import com.mv.cinemaservice.infrastructure.persistence.entity.SeatEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatJpaRepository extends JpaRepository<SeatEntity, UUID> {

    @EntityGraph(attributePaths = {"seatType"})
    List<SeatEntity> findByScreenIdOrderByRowLabelAscSeatNumberAsc(UUID screenId);

    /** Bulk delete; flushed and cleared so the new seats (same labels) can be inserted in the same transaction. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SeatEntity s WHERE s.screen.id = :screenId")
    void deleteByScreenId(@Param("screenId") UUID screenId);
}
