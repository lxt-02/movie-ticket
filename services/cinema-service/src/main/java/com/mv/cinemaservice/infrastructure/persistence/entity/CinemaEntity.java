package com.mv.cinemaservice.infrastructure.persistence.entity;

import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "cinemas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "address", nullable = false, columnDefinition = "text")
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "phone", length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CinemaStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
