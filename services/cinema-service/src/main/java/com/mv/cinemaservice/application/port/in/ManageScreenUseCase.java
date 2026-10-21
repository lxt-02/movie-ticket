package com.mv.cinemaservice.application.port.in;

import com.mv.cinemaservice.application.command.SaveScreenCommand;
import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;
import com.mv.cinemaservice.domain.model.screen.entity.Seat;
import com.mv.cinemaservice.domain.model.screen.enums.ScreenStatus;
import com.mv.cinemaservice.domain.model.screen.enums.SeatStatus;
import com.mv.cinemaservice.domain.model.screen.valueobject.SeatRow;

import java.util.List;
import java.util.UUID;

/** Admin operations on screens and their seat layout. */
public interface ManageScreenUseCase {
    ScreenLayoutResult create(UUID cinemaId, SaveScreenCommand command);

    Screen update(UUID screenId, SaveScreenCommand command);

    Screen changeStatus(UUID screenId, ScreenStatus status);

    /** Replaces every seat (screen must not be ACTIVE). */
    ScreenLayoutResult replaceLayout(UUID screenId, List<SeatRow> rows);

    Seat changeSeatStatus(UUID screenId, UUID seatId, SeatStatus status);
}
