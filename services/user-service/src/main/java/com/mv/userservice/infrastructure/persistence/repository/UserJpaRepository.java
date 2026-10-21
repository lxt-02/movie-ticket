package com.mv.userservice.infrastructure.persistence.repository;

import com.mv.userservice.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    @org.springframework.data.jpa.repository.Query("""
            SELECT u FROM UserEntity u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
              AND (:keyword IS NULL
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    org.springframework.data.domain.Page<UserEntity> search(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("role")
            com.mv.userservice.domain.model.user.enums.UserRole role,
            @org.springframework.data.repository.query.Param("status")
            com.mv.userservice.domain.model.user.enums.UserStatus status,
            org.springframework.data.domain.Pageable pageable);
}
