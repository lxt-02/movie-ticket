package com.mv.showtimeservice.infrastructure.persistence.repository;

import com.mv.showtimeservice.infrastructure.persistence.entity.ShowtimeEntity;
import jakarta.persistence.LockModeType;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShowtimeJpaRepository extends JpaRepository<ShowtimeEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ShowtimeEntity s WHERE s.id = :id")
    Optional<ShowtimeEntity> findByIdWithLock(@Param("id") UUID id);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM ShowtimeEntity s
            WHERE s.screenId = :screenId
              AND s.status <> :cancelled
              AND (:excludeId IS NULL OR s.id <> :excludeId)
              AND s.startTime < :to
              AND s.endTime > :from
            """)
    boolean existsOverlapping(@Param("screenId") UUID screenId,
                              @Param("from") Instant from,
                              @Param("to") Instant to,
                              @Param("excludeId") UUID excludeId,
                              @Param("cancelled") ShowtimeStatus cancelled);

    /** SCHEDULED -> SELLING once the sales window opens (start - window <= now), for showtimes not started yet. */
    @Modifying
    @Query("""
            UPDATE ShowtimeEntity s SET s.status = :selling
            WHERE s.status = :scheduled AND s.startTime <= :opensBefore AND s.startTime > :now
            """)
    int openSales(@Param("opensBefore") Instant opensBefore, @Param("now") Instant now,
                  @Param("scheduled") ShowtimeStatus scheduled, @Param("selling") ShowtimeStatus selling);

    /** SCHEDULED / SELLING -> ENDED after the end time. */
    @Modifying
    @Query("""
            UPDATE ShowtimeEntity s SET s.status = :ended
            WHERE s.status IN :running AND s.endTime < :now
            """)
    int endFinished(@Param("now") Instant now, @Param("running") Collection<ShowtimeStatus> running,
                    @Param("ended") ShowtimeStatus ended);

    @Query("""
            SELECT s FROM ShowtimeEntity s
            WHERE (:movieId IS NULL OR s.movieId = :movieId)
              AND (:cinemaId IS NULL OR s.cinemaId = :cinemaId)
              AND (:screenId IS NULL OR s.screenId = :screenId)
              AND (:from IS NULL OR s.startTime >= :from)
              AND (:to IS NULL OR s.startTime < :to)
              AND s.status IN :statuses
            """)
    Page<ShowtimeEntity> search(@Param("movieId") UUID movieId,
                                @Param("cinemaId") UUID cinemaId,
                                @Param("screenId") UUID screenId,
                                @Param("from") Instant from,
                                @Param("to") Instant to,
                                @Param("statuses") Collection<ShowtimeStatus> statuses,
                                Pageable pageable);
}
