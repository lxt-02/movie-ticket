package com.mv.bookingservice.infrastructure.persistence.repository;

import com.mv.bookingservice.infrastructure.persistence.entity.OutboxMessageInfraEntity;
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
public interface OutboxMessageJpaRepository extends JpaRepository<OutboxMessageInfraEntity, UUID> {

    /**
     * PENDING rows, oldest first (per-booking order is the insert order). A row is left out while an older row of
     * the same booking is FAILED: CANCEL_PAYMENT must never overtake a CREATE_PAYMENT that was not delivered.
     */
    @Query(value = """
            SELECT o.* FROM outbox_messages o
            WHERE o.status = 'PENDING'
              AND NOT EXISTS (SELECT 1 FROM outbox_messages f
                              WHERE f.aggregate_type = o.aggregate_type
                                AND f.aggregate_id = o.aggregate_id
                                AND f.status = 'FAILED'
                                AND f.created_at <= o.created_at)
            ORDER BY o.created_at ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<OutboxMessageInfraEntity> findPublishable(@Param("limit") int limit);

    long countByStatus(String status);

    /** PENDING rows that wait behind a FAILED row of the same booking. */
    @Query(value = """
            SELECT COUNT(*) FROM outbox_messages o
            WHERE o.status = 'PENDING'
              AND EXISTS (SELECT 1 FROM outbox_messages f
                          WHERE f.aggregate_type = o.aggregate_type
                            AND f.aggregate_id = o.aggregate_id
                            AND f.status = 'FAILED'
                            AND f.created_at <= o.created_at)
            """, nativeQuery = true)
    long countHeldBehindFailed();

    Optional<OutboxMessageInfraEntity> findFirstByStatusOrderByCreatedAtAsc(String status);

    List<OutboxMessageInfraEntity> findTop20ByStatusOrderByCreatedAtAsc(String status);

    /** FAILED -> PENDING (after the cause was fixed). Returns the number of rows requeued. */
    @Modifying
    @Query("""
            UPDATE OutboxMessageInfraEntity o
            SET o.status = 'PENDING', o.retryCount = 0, o.nextRetryAt = null
            WHERE o.status = 'FAILED'
            """)
    int requeueFailed();

    /** Oldest row with this status (age of the backlog), null when there is none. */
    @Query("SELECT MIN(o.createdAt) FROM OutboxMessageInfraEntity o WHERE o.status = :status")
    Instant oldestCreatedAt(@Param("status") String status);
}
