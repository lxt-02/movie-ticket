package com.mv.bookingservice.infrastructure.persistence.repository;

import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import com.mv.bookingservice.infrastructure.persistence.entity.BookingEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID> {

    @EntityGraph(attributePaths = {"seats"})
    Optional<BookingEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"seats"})
    Optional<BookingEntity> findByBookingCode(String bookingCode);

    @EntityGraph(attributePaths = {"seats"})
    Optional<BookingEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    @EntityGraph(attributePaths = {"seats"})
    Optional<BookingEntity> findByHoldId(UUID holdId);

    @EntityGraph(attributePaths = {"seats"})
    List<BookingEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"seats"})
    List<BookingEntity> findByStatusAndExpiresAtBefore(BookingStatus status, Instant time);
}
