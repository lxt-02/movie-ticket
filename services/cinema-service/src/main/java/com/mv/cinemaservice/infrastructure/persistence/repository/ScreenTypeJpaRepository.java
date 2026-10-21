package com.mv.cinemaservice.infrastructure.persistence.repository;

import com.mv.cinemaservice.infrastructure.persistence.entity.ScreenTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScreenTypeJpaRepository extends JpaRepository<ScreenTypeEntity, UUID> {
}
