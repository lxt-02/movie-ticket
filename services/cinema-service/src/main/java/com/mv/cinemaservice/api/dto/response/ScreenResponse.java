package com.mv.cinemaservice.api.dto.response;

import com.mv.cinemaservice.domain.model.screen.enums.ScreenStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenResponse {
    private UUID id;
    private UUID cinemaId;
    private UUID screenTypeId;
    private String screenTypeName;
    private String name;
    private int totalSeats;
    private ScreenStatus status;
}
