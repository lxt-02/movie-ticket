package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.command.ConfirmHoldCommand;
import com.mv.showtimeservice.application.port.in.ConfirmHoldUseCase;
import com.mv.showtimeservice.application.port.out.SaveSeatHoldPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSeatRepositoryPort;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;
import com.mv.showtimeservice.domain.model.seathold.enums.SeatHoldStatus;
import com.mv.showtimeservice.domain.model.seathold.exception.SeatHoldConflictException;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeSeatStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * HELD -> CONFIRMED and the hold's seats -> BOOKED, after the booking was paid.
 * <p>
 * Contract for booking-service:
 * <ul>
 *     <li>200: the hold is CONFIRMED (also when it already was: idempotent).</li>
 *     <li>404: unknown hold.</li>
 *     <li>409: the seats can no longer be confirmed (hold released / expired, or a seat now belongs to someone
 *     else). This is permanent: retrying will not help, the paid booking must be refunded.</li>
 * </ul>
 * A failure never answers 200, so the caller can never mistake "seats released" for "seats booked".
 * Nothing is changed on 409. The showtime, hold and seat rows are locked first ({@link SeatHoldLocks}).
 */
@Service
@RequiredArgsConstructor
public class ConfirmHoldService implements ConfirmHoldUseCase {

    private final SeatHoldLocks seatHoldLocks;
    private final SaveSeatHoldPort saveSeatHoldPort;
    private final ShowtimeSeatRepositoryPort showtimeSeatRepositoryPort;

    @Override
    @Transactional
    public SeatHold execute(ConfirmHoldCommand command) {
        SeatHoldLocks.LockedHold locked = seatHoldLocks.lock(command.getHoldId());
        SeatHold hold = locked.hold();

        if (hold.getStatus() == SeatHoldStatus.CONFIRMED) {
            return hold; // Idempotent confirm
        }
        if (hold.getStatus() != SeatHoldStatus.HELD) {
            throw new SeatHoldConflictException("Hold " + hold.getId() + " is " + hold.getStatus()
                    + ", its seats can no longer be confirmed");
        }
        if (hold.isExpired()) {
            throw new SeatHoldConflictException("Hold " + hold.getId() + " expired at " + hold.getExpiresAt()
                    + ", its seats can no longer be confirmed");
        }

        List<ShowtimeSeat> seats = locked.seats();
        int expectedSeats = hold.getItems() == null ? 0 : hold.getItems().size();
        if (seats.isEmpty() || seats.size() != expectedSeats) {
            throw new SeatHoldConflictException("Hold " + hold.getId() + " expects " + expectedSeats
                    + " seat(s) but found " + seats.size());
        }
        for (ShowtimeSeat seat : seats) {
            if (!hold.getId().equals(seat.getHoldId()) || seat.getStatus() != ShowtimeSeatStatus.HELD) {
                throw new SeatHoldConflictException("Seat " + seat.getSeatLabel() + " is no longer held by hold "
                        + hold.getId() + " (status " + seat.getStatus() + ")");
            }
        }

        hold.confirm();
        for (ShowtimeSeat seat : seats) {
            seat.book();
        }
        showtimeSeatRepositoryPort.saveAll(seats);
        return saveSeatHoldPort.save(hold);
    }
}
