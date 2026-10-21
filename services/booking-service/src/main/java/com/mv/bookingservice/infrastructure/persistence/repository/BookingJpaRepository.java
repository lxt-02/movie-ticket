package com.mv.bookingservice.infrastructure.persistence.repository;

import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import com.mv.bookingservice.infrastructure.persistence.entity.BookingEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** Expiry job: overdue PENDING bookings, skipping rows locked by another instance. */
    @Query(value = """
            SELECT * FROM bookings
            WHERE status = 'PENDING'
              AND expires_at IS NOT NULL
              AND expires_at < :cutoff
            ORDER BY expires_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<BookingEntity> findOverduePendingForUpdate(@Param("cutoff") Instant cutoff, @Param("limit") int limit);

    /** Confirmation job: paid bookings whose seats are not confirmed yet. */
    @Query(value = """
            SELECT * FROM bookings
            WHERE status = 'CONFIRMING'
            ORDER BY updated_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<BookingEntity> findConfirmingForUpdate(@Param("limit") int limit);

    /** Bookings with money to give back by hand (see V5__saga_reliability.sql). */
    @EntityGraph(attributePaths = {"seats"})
    List<BookingEntity> findByNeedsRefundTrueOrderByUpdatedAtDesc();
}
