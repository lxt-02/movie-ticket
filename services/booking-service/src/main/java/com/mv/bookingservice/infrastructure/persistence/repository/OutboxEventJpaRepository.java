package com.mv.bookingservice.infrastructure.persistence.repository;

import com.mv.bookingservice.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findByPublishedAtIsNullOrderByCreatedAtAsc();

    /** Oldest unpublished rows first (per-booking order is the insert order). */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE published_at IS NULL
            ORDER BY created_at ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<OutboxEventEntity> findUnpublished(@Param("limit") int limit);
}
