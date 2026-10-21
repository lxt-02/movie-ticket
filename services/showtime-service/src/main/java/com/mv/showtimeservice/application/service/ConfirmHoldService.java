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
 *     else). This is permanent: retrying will not help, the paid booking is cancelled.</li>
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
            throw new SeatHoldConflictException("Lượt giữ ghế " + hold.getId() + " đang ở trạng thái " + hold.getStatus()
                    + ", không thể xác nhận ghế nữa");
        }
        if (hold.isExpired()) {
            throw new SeatHoldConflictException("Lượt giữ ghế " + hold.getId() + " đã hết hạn lúc " + hold.getExpiresAt()
                    + ", không thể xác nhận ghế nữa");
        }

        List<ShowtimeSeat> seats = locked.seats();
        int expectedSeats = hold.getItems() == null ? 0 : hold.getItems().size();
        if (seats.isEmpty() || seats.size() != expectedSeats) {
            throw new SeatHoldConflictException("Lượt giữ ghế " + hold.getId() + " cần " + expectedSeats
                    + " ghế nhưng chỉ tìm thấy " + seats.size());
        }
        for (ShowtimeSeat seat : seats) {
            if (!hold.getId().equals(seat.getHoldId()) || seat.getStatus() != ShowtimeSeatStatus.HELD) {
                throw new SeatHoldConflictException("Ghế " + seat.getSeatLabel() + " không còn được giữ bởi lượt "
                        + hold.getId() + " (trạng thái " + seat.getStatus() + ")");
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
