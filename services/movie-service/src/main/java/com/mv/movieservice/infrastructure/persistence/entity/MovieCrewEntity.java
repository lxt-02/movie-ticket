package com.mv.movieservice.infrastructure.persistence.entity;

import com.mv.movieservice.domain.model.movie.enums.CrewRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * One row of movie_crew (PK movie_id + person_id + role). Saved by replacing all rows of a movie
 * (see MovieRepositoryAdapter), the person's name is read from persons.
 */
@Entity
@Table(name = "movie_crew")
@IdClass(MovieCrewEntity.Key.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieCrewEntity {

    @Id
    @Column(name = "movie_id", nullable = false)
    private UUID movieId;

    @Id
    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private CrewRole role;

    @Column(name = "character_name", length = 150)
    private String characterName;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private UUID movieId;
        private UUID personId;
        private CrewRole role;
    }
}
