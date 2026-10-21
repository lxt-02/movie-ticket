package com.mv.movieservice.infrastructure.persistence.repository;

import com.mv.movieservice.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /** Oldest unpublished rows first (per-movie order is the insert order). */
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.publishedAt IS NULL ORDER BY e.createdAt ASC")
    List<OutboxEventEntity> findUnpublished(Pageable pageable);
}
