package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.command.ConfirmHoldCommand;
import com.mv.showtimeservice.application.port.in.ConfirmHoldUseCase;
import com.mv.showtimeservice.application.port.out.LoadSeatHoldPort;
import com.mv.showtimeservice.application.port.out.SaveSeatHoldPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSeatRepositoryPort;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import com.mv.showtimeservice.domain.model.seathold.exception.SeatHoldNotFoundException;
import com.mv.showtimeservice.domain.model.seathold.enums.SeatHoldStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfirmHoldService implements ConfirmHoldUseCase {

    private final LoadSeatHoldPort loadSeatHoldPort;
    private final SaveSeatHoldPort saveSeatHoldPort;
    private final ShowtimeSeatRepositoryPort showtimeSeatRepositoryPort;

    @Override
    @Transactional
    public SeatHold execute(ConfirmHoldCommand command) {
        SeatHold hold = loadSeatHoldPort.findById(command.getHoldId())
                .orElseThrow(() -> new SeatHoldNotFoundException(command.getHoldId()));

        if (hold.getStatus() == SeatHoldStatus.CONFIRMED) {
            return hold; // Idempotent confirm
        }

        hold.confirm();

        List<ShowtimeSeat> seats = showtimeSeatRepositoryPort.findByHoldId(hold.getId());
        for (ShowtimeSeat seat : seats) {
            seat.book();
        }

        showtimeSeatRepositoryPort.saveAll(seats);
        return saveSeatHoldPort.save(hold);
    }
}
