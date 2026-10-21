package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.command.ReleaseHoldCommand;
import com.mv.showtimeservice.application.port.in.ReleaseHoldUseCase;
import com.mv.showtimeservice.application.port.out.SaveSeatHoldPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSeatRepositoryPort;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;
import com.mv.showtimeservice.domain.model.seathold.enums.SeatHoldStatus;
import com.mv.showtimeservice.domain.model.seathold.exception.SeatHoldConflictException;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * HELD -> RELEASED and frees the seats this hold still owns. Seats already reclaimed by another hold are left
 * alone. The showtime, hold and seat rows are locked first ({@link SeatHoldLocks}).
 * <ul>
 *     <li>RELEASED: idempotent, 200.</li>
 *     <li>EXPIRED: also 200 (the seats are free either way); any seat still pointing at the hold is freed.</li>
 *     <li>CONFIRMED: 409, booked seats are never released this way.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ReleaseHoldService implements ReleaseHoldUseCase {

    private final SeatHoldLocks seatHoldLocks;
    private final SaveSeatHoldPort saveSeatHoldPort;
    private final ShowtimeSeatRepositoryPort showtimeSeatRepositoryPort;

    @Override
    @Transactional
    public SeatHold execute(ReleaseHoldCommand command) {
        SeatHoldLocks.LockedHold locked = seatHoldLocks.lock(command.getHoldId());
        SeatHold hold = locked.hold();

        if (hold.getStatus() == SeatHoldStatus.RELEASED) {
            return hold; // Idempotent release
        }
        if (hold.getStatus() == SeatHoldStatus.CONFIRMED) {
            throw new SeatHoldConflictException("Lượt giữ ghế " + hold.getId() + " đã xác nhận (CONFIRMED), ghế đã được bán");
        }

        List<ShowtimeSeat> owned = locked.seats().stream()
                .filter(seat -> hold.getId().equals(seat.getHoldId()))
                .toList();
        owned.forEach(ShowtimeSeat::release);
        showtimeSeatRepositoryPort.saveAll(owned);

        if (hold.getStatus() == SeatHoldStatus.EXPIRED) {
            return hold; // Already ended: nothing to change on the hold itself
        }
        hold.release();
        return saveSeatHoldPort.save(hold);
    }
}
