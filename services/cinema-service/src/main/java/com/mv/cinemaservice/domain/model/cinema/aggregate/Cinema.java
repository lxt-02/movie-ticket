package com.mv.cinemaservice.domain.model.cinema.aggregate;

import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cinema {
    private UUID id;
    private String name;
    private String brand;
    private String address;
    private String city;
    private String district;
    private String phone;
    private CinemaStatus status;
    private Instant createdAt;

    public boolean isActive() {
        return status == CinemaStatus.ACTIVE;
    }
}
