package com.mv.bookingservice.infrastructure.persistence.repository;

import com.mv.bookingservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.mv.bookingservice.infrastructure.persistence.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findByPublishedAtIsNullOrderByCreatedAtAsc();

    /**
     * PENDING rows, oldest first (per-booking order is the insert order). A row is left out while an older row of
     * the same booking is FAILED: CANCEL_PAYMENT must never overtake a CREATE_PAYMENT that was not delivered.
     */
    @Query(value = """
            SELECT o.* FROM outbox_events o
            WHERE o.status = 'PENDING'
              AND NOT EXISTS (SELECT 1 FROM outbox_events f
                              WHERE f.aggregate_id = o.aggregate_id
                                AND f.status = 'FAILED'
                                AND f.created_at <= o.created_at)
            ORDER BY o.created_at ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<OutboxEventEntity> findPublishable(@Param("limit") int limit);

    long countByStatus(OutboxStatus status);

    /** PENDING rows that wait behind a FAILED row of the same booking. */
    @Query(value = """
            SELECT COUNT(*) FROM outbox_events o
            WHERE o.status = 'PENDING'
              AND EXISTS (SELECT 1 FROM outbox_events f
                          WHERE f.aggregate_id = o.aggregate_id
                            AND f.status = 'FAILED'
                            AND f.created_at <= o.created_at)
            """, nativeQuery = true)
    long countHeldBehindFailed();

    Optional<OutboxEventEntity> findFirstByStatusOrderByCreatedAtAsc(OutboxStatus status);

    List<OutboxEventEntity> findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    /** FAILED -> PENDING (after the cause was fixed). Returns the number of rows requeued. */
    @Modifying
    @Query("""
            UPDATE OutboxEventEntity o
            SET o.status = com.mv.bookingservice.infrastructure.persistence.entity.OutboxStatus.PENDING,
                o.retryCount = 0, o.nextRetryAt = null
            WHERE o.status = com.mv.bookingservice.infrastructure.persistence.entity.OutboxStatus.FAILED
            """)
    int requeueFailed();

    /** Oldest PENDING row (age of the backlog), null when there is none. */
    @Query("SELECT MIN(o.createdAt) FROM OutboxEventEntity o WHERE o.status = :status")
    Instant oldestCreatedAt(@Param("status") OutboxStatus status);
}
