package com.mv.cinemaservice.application.service;

import com.mv.cinemaservice.application.command.SaveCinemaCommand;
import com.mv.cinemaservice.application.port.in.ManageCinemaUseCase;
import com.mv.cinemaservice.application.port.out.LoadCinemaPort;
import com.mv.cinemaservice.application.port.out.SaveCinemaPort;
import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;
import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;
import com.mv.cinemaservice.domain.model.cinema.exception.CinemaNotFoundException;
import com.mv.cinemaservice.domain.model.cinema.exception.InvalidCinemaException;
import com.mv.cinemaservice.domain.model.cinema.valueobject.CinemaDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ManageCinemaService implements ManageCinemaUseCase {

    private final LoadCinemaPort loadCinemaPort;
    private final SaveCinemaPort saveCinemaPort;
    private final Clock clock;

    @Override
    public Cinema create(SaveCinemaCommand command) {
        return saveCinemaPort.save(Cinema.create(toDetails(command), command.getStatus(), clock.instant()));
    }

    @Override
    public Cinema update(UUID cinemaId, SaveCinemaCommand command) {
        Cinema cinema = load(cinemaId);
        cinema.update(toDetails(command));
        return saveCinemaPort.save(cinema);
    }

    @Override
    public Cinema changeStatus(UUID cinemaId, CinemaStatus status) {
        Cinema cinema = load(cinemaId);
        cinema.changeStatus(status);
        return saveCinemaPort.save(cinema);
    }

    @Override
    public Cinema close(UUID cinemaId) {
        Cinema cinema = load(cinemaId);
        cinema.close();
        return saveCinemaPort.save(cinema);
    }

    private Cinema load(UUID cinemaId) {
        return loadCinemaPort.findById(cinemaId).orElseThrow(() -> new CinemaNotFoundException(cinemaId));
    }

    private static CinemaDetails toDetails(SaveCinemaCommand c) {
        if (c == null) {
            throw new InvalidCinemaException("Thiếu dữ liệu rạp");
        }
        return new CinemaDetails(c.getName(), c.getBrand(), c.getAddress(), c.getCity(), c.getDistrict(), c.getPhone());
    }
}
