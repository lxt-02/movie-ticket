package com.mv.cinemaservice.application.port.in;

import com.mv.cinemaservice.application.command.SaveCinemaCommand;
import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;
import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;

import java.util.UUID;

/** Admin operations on cinemas. */
public interface ManageCinemaUseCase {
    Cinema create(SaveCinemaCommand command);

    Cinema update(UUID cinemaId, SaveCinemaCommand command);

    Cinema changeStatus(UUID cinemaId, CinemaStatus status);

    /** Soft delete: the cinema becomes CLOSED. */
    Cinema close(UUID cinemaId);
}
