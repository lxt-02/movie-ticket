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

    /** Stands for "no screen to exclude" (never a real id). */
    UUID NO_ID = new UUID(0L, 0L);

    /**
     * {@code excludeId} null = check every screen. A null UUID is replaced by {@link #NO_ID} instead of
     * "(:excludeId IS NULL OR ...)", which PostgreSQL rejects ("could not determine data type of parameter").
     */
    default boolean existsByName(UUID cinemaId, String name, UUID excludeId) {
        return existsByNameExcluding(cinemaId, name, excludeId != null ? excludeId : NO_ID);
    }

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM ScreenEntity s
            WHERE s.cinema.id = :cinemaId AND LOWER(s.name) = LOWER(:name)
              AND s.id <> :excludeId
            """)
    boolean existsByNameExcluding(@Param("cinemaId") UUID cinemaId, @Param("name") String name,
                                  @Param("excludeId") UUID excludeId);
}
