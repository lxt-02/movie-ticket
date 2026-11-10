package com.mv.cinemaservice.infrastructure.persistence.repository;

import com.mv.cinemaservice.infrastructure.persistence.entity.CinemaEntity;
import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CinemaJpaRepository extends JpaRepository<CinemaEntity, UUID> {

    @Query("""
            SELECT c FROM CinemaEntity c
            WHERE (:city = '' OR LOWER(c.city) = LOWER(:city))
              AND (:status IS NULL OR c.status = :status)
              AND (:status IS NOT NULL OR :includeClosed = TRUE OR c.status <> :closed)
              AND (LOWER(c.name) LIKE CONCAT('%', LOWER(:keyword), '%')
                   OR LOWER(c.address) LIKE CONCAT('%', LOWER(:keyword), '%'))
            """)
    Page<CinemaEntity> search(@Param("city") String city,
                              @Param("status") CinemaStatus status,
                              @Param("keyword") String keyword,
                              @Param("includeClosed") boolean includeClosed,
                              @Param("closed") CinemaStatus closed,
                              Pageable pageable);
}
