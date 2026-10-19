package com.mv.movieservice.infrastructure.persistence.repository;

import com.mv.movieservice.infrastructure.persistence.entity.MovieEntity;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MovieJpaRepository extends JpaRepository<MovieEntity, UUID> {

    @Query("""
            SELECT m FROM MovieEntity m
            WHERE (:status IS NULL OR m.status = :status)
              AND (:status IS NOT NULL OR :includeArchived = TRUE OR m.status <> :archived)
              AND (:keyword IS NULL
                   OR LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.originalTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<MovieEntity> search(@Param("status") MovieStatus status,
                             @Param("keyword") String keyword,
                             @Param("includeArchived") boolean includeArchived,
                             @Param("archived") MovieStatus archived,
                             Pageable pageable);
}
