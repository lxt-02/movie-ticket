package com.mv.movieservice.infrastructure.persistence.repository;

import com.mv.movieservice.infrastructure.persistence.entity.PersonEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PersonJpaRepository extends JpaRepository<PersonEntity, UUID> {

    @Query("""
            SELECT p FROM PersonEntity p
            WHERE :keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<PersonEntity> search(@Param("keyword") String keyword, Pageable pageable);
}
