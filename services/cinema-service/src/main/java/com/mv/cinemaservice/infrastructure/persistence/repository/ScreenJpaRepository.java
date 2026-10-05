package com.mv.cinemaservice.infrastructure.persistence.repository;

import com.mv.cinemaservice.infrastructure.persistence.entity.ScreenEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScreenJpaRepository extends JpaRepository<ScreenEntity, UUID> {

    @EntityGraph(attributePaths = {"cinema", "screenType"})
    Optional<ScreenEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"cinema", "screenType"})
    List<ScreenEntity> findByCinemaId(UUID cinemaId);
}
