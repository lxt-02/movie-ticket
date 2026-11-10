package com.mv.showtimeservice.infrastructure.persistence.repository;

import com.mv.showtimeservice.infrastructure.persistence.entity.ScreenSnapshotEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScreenSnapshotJpaRepository extends JpaRepository<ScreenSnapshotEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ScreenSnapshotEntity s WHERE s.screenId = :screenId")
    Optional<ScreenSnapshotEntity> findByIdForUpdate(@Param("screenId") UUID screenId);
}
