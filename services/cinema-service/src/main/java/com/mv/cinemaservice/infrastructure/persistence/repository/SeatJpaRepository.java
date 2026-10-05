package com.mv.cinemaservice.infrastructure.persistence.repository;

import com.mv.cinemaservice.infrastructure.persistence.entity.SeatEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatJpaRepository extends JpaRepository<SeatEntity, UUID> {

    @EntityGraph(attributePaths = {"seatType"})
    List<SeatEntity> findByScreenIdOrderByRowLabelAscSeatNumberAsc(UUID screenId);
}
