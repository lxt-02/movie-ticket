package com.mv.showtimeservice.infrastructure.persistence.repository;

import com.mv.showtimeservice.infrastructure.persistence.entity.SeatHoldEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatHoldJpaRepository extends JpaRepository<SeatHoldEntity, UUID> {

    @EntityGraph(attributePaths = {"items"})
    Optional<SeatHoldEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"items"})
    Optional<SeatHoldEntity> findByBookingId(UUID bookingId);

    /** Scalar query: does not put the hold into the persistence context, so the locked read below is fresh. */
    @Query("SELECT h.showtimeId FROM SeatHoldEntity h WHERE h.id = :id")
    Optional<UUID> findShowtimeIdById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT h FROM SeatHoldEntity h LEFT JOIN FETCH h.items WHERE h.id = :id")
    Optional<SeatHoldEntity> findByIdForUpdate(@Param("id") UUID id);
}
