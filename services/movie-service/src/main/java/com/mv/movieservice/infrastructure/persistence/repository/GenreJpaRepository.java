package com.mv.movieservice.infrastructure.persistence.repository;

import com.mv.movieservice.infrastructure.persistence.entity.GenreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GenreJpaRepository extends JpaRepository<GenreEntity, UUID> {

    boolean existsByNameIgnoreCase(String name);
}
