package com.mv.cinemaservice.api.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A screen. status: ACTIVE / MAINTENANCE / INACTIVE. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenDto {
    private UUID id;
    private UUID cinemaId;
    private UUID screenTypeId;
    private String screenTypeName;
    private String name;
    private int totalSeats;
    private String status;
}
