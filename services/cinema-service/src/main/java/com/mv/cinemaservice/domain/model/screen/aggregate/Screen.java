package com.mv.cinemaservice.domain.model.screen.aggregate;

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
public class Screen {
    private UUID id;
    private UUID cinemaId;
    private UUID screenTypeId;
    private String screenTypeName;
    private String name;
    private int totalSeats;
    private ScreenStatus status;

    public boolean isActive() {
        return status == ScreenStatus.ACTIVE;
    }
}
