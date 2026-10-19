package com.mv.paymentservice.infrastructure.persistence.repository;

import com.mv.paymentservice.infrastructure.persistence.entity.OutboxMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * MySQL has no UPDATE ... RETURNING, so the relay locks with this query and updates in the same transaction.
     */
    @Query(value = """
            SELECT * FROM outbox_messages
            WHERE retry_count < :maxAttempts
              AND ( (status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= :now))
                 OR (status = 'PROCESSING' AND next_retry_at IS NOT NULL AND next_retry_at <= :now) )
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxMessageEntity> findClaimableForUpdate(@Param("maxAttempts") int maxAttempts,
                                                     @Param("now") Instant now,
                                                     @Param("limit") int limit);
}
