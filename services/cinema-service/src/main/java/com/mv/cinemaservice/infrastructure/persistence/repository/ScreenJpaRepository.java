package com.mv.cinemaservice.infrastructure.persistence.repository;

import com.mv.cinemaservice.infrastructure.persistence.entity.ScreenEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM ScreenEntity s
            WHERE s.cinema.id = :cinemaId AND LOWER(s.name) = LOWER(:name)
              AND (:excludeId IS NULL OR s.id <> :excludeId)
            """)
    boolean existsByName(@Param("cinemaId") UUID cinemaId, @Param("name") String name,
                         @Param("excludeId") UUID excludeId);
}
