package com.mv.paymentservice.infrastructure.persistence.repository;

import com.mv.paymentservice.infrastructure.persistence.entity.OutboxMessageEntity;
import com.mv.paymentservice.infrastructure.persistence.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxMessageJpaRepository extends JpaRepository<OutboxMessageEntity, UUID> {

    /**
     * Claimable messages: PENDING ready for (re)try, or PROCESSING stuck past its deadline
     * (instance died mid-publish). SKIP LOCKED makes it safe with several instances.
     * The relay locks with this query and updates the rows in the same transaction (portable, no UPDATE ... RETURNING).
     * FAILED rows (permanent error) are never claimed: they wait for POST /api/v1/payments/admin/outbox/requeue-failed.
     */
    @Query(value = """
            SELECT * FROM outbox_messages
            WHERE (status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= :now))
               OR (status = 'PROCESSING' AND next_retry_at IS NOT NULL AND next_retry_at <= :now)
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxMessageEntity> findClaimableForUpdate(@Param("now") Instant now,
                                                     @Param("limit") int limit);

    long countByStatus(OutboxStatus status);

    List<OutboxMessageEntity> findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Query("SELECT MIN(m.createdAt) FROM OutboxMessageEntity m WHERE m.status = :status")
    Instant oldestCreatedAt(@Param("status") OutboxStatus status);

    /** FAILED -> PENDING once the cause is fixed. Consumers dedupe by eventId, so a re-send is harmless. */
    @Modifying
    @Query("""
            UPDATE OutboxMessageEntity m
            SET m.status = com.mv.paymentservice.infrastructure.persistence.entity.OutboxStatus.PENDING,
                m.retryCount = 0, m.nextRetryAt = null
            WHERE m.status = com.mv.paymentservice.infrastructure.persistence.entity.OutboxStatus.FAILED
            """)
    int requeueFailed();
}
