package com.mv.showtimeservice.infrastructure.persistence.repository;

import com.mv.showtimeservice.infrastructure.persistence.entity.SeatHoldEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatHoldJpaRepository extends JpaRepository<SeatHoldEntity, UUID> {

    @EntityGraph(attributePaths = {"items"})
    Optional<SeatHoldEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"items"})
    Optional<SeatHoldEntity> findByBookingId(UUID bookingId);

    @EntityGraph(attributePaths = {"items"})
    Optional<SeatHoldEntity> findByIdempotencyKey(String idempotencyKey);
}
