package com.mv.showtimeservice.infrastructure.client.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A cinema. status: ACTIVE / INACTIVE / CLOSED. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaDto {
    private UUID id;
    private String name;
    private String brand;
    private String address;
    private String city;
    private String district;
    private String phone;
    private String status;
    private Instant createdAt;
}
