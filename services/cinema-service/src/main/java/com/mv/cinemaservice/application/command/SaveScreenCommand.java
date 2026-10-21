package com.mv.cinemaservice.application.command;

import com.mv.cinemaservice.domain.model.screen.valueobject.SeatRow;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/** Create a screen (name, type, layout) or update name / type ({@code rows} ignored on update). */
@Getter
@Builder
public class SaveScreenCommand {
    private String name;
    private UUID screenTypeId;
    private List<SeatRow> rows;
}
