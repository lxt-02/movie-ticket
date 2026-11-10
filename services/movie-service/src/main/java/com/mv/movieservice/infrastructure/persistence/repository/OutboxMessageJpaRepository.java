package com.mv.movieservice.infrastructure.persistence.repository;

import com.mv.movieservice.infrastructure.persistence.entity.OutboxMessageInfraEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxMessageJpaRepository extends JpaRepository<OutboxMessageInfraEntity, UUID> {

    /** PENDING rows, oldest first (per-movie order is the insert order). */
    @Query("SELECT m FROM OutboxMessageInfraEntity m WHERE m.status = 'PENDING' ORDER BY m.createdAt ASC")
    List<OutboxMessageInfraEntity> findPending(Pageable pageable);

    long countByStatus(String status);
}
