package com.mv.showtimeservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movie_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieSnapshotEntity {

    @Id
    @Column(name = "movie_id", nullable = false)
    private UUID movieId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "age_rating", length = 20)
    private String ageRating;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
