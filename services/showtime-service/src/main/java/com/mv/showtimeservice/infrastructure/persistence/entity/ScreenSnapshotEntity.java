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
@Table(name = "screen_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenSnapshotEntity {

    @Id
    @Column(name = "screen_id", nullable = false)
    private UUID screenId;

    @Column(name = "cinema_id", nullable = false)
    private UUID cinemaId;

    @Column(name = "cinema_name", nullable = false, length = 255)
    private String cinemaName;

    @Column(name = "screen_name", nullable = false, length = 100)
    private String screenName;

    @Column(name = "screen_type", length = 50)
    private String screenType;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
