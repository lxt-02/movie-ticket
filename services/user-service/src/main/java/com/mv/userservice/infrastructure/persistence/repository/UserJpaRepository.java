package com.mv.userservice.infrastructure.persistence.repository;

import com.mv.userservice.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    /** Case-insensitive like the old MySQL collation (PostgreSQL compares text case-sensitively). */
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @org.springframework.data.jpa.repository.Query("""
            SELECT u FROM UserEntity u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
              AND (LOWER(u.email) LIKE :pattern OR LOWER(u.fullName) LIKE :pattern)
            """)
    /**
     * @param pattern lower-case LIKE pattern, never null ("%" = no keyword). A null String parameter is sent to
     *                PostgreSQL as bytea and LOWER(bytea) fails, so the pattern is built in Java instead.
     */
    org.springframework.data.domain.Page<UserEntity> search(
            @org.springframework.data.repository.query.Param("pattern") String pattern,
            @org.springframework.data.repository.query.Param("role")
            com.mv.userservice.domain.model.user.enums.UserRole role,
            @org.springframework.data.repository.query.Param("status")
            com.mv.userservice.domain.model.user.enums.UserStatus status,
            org.springframework.data.domain.Pageable pageable);
}
