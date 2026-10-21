package com.mv.movieservice.infrastructure.persistence.repository;

import com.mv.movieservice.infrastructure.persistence.entity.MovieCrewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface MovieCrewJpaRepository extends JpaRepository<MovieCrewEntity, MovieCrewEntity.Key> {

    List<MovieCrewEntity> findByMovieIdIn(Collection<UUID> movieIds);

    /**
     * Bulk delete of a movie's crew. Pending changes are flushed first (the movie row must exist before its crew
     * is re-inserted) and the persistence context is cleared afterwards, so crew rows loaded earlier in the same
     * transaction are not merged back as stale updates.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM MovieCrewEntity c WHERE c.movieId = :movieId")
    void deleteByMovieId(@Param("movieId") UUID movieId);
}
