package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.command.UpdateShowtimeCommand;
import com.mv.showtimeservice.application.dto.ShowtimeSchedulePolicy;
import com.mv.showtimeservice.application.port.in.ManageShowtimeUseCase;
import com.mv.showtimeservice.application.port.out.ShowtimeRepositoryPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSeatRepositoryPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSnapshotPort;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import com.mv.showtimeservice.domain.model.showtime.exception.InvalidShowtimeException;
import com.mv.showtimeservice.domain.model.showtime.exception.ShowtimeConflictException;
import com.mv.showtimeservice.domain.model.showtime.exception.ShowtimeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Update / cancel a showtime.
 * <p>
 * The showtime row is locked first (same as {@link HoldSeatsService} and seat confirm / release), so nobody can
 * hold a seat while the admin is changing the showtime. Changes are refused once any seat is sold or held by a
 * live hold: buyers paid for a given time and price, and there is no refund flow.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ManageShowtimeService implements ManageShowtimeUseCase {

    private final ShowtimeRepositoryPort showtimeRepositoryPort;
    private final ShowtimeSeatRepositoryPort showtimeSeatRepositoryPort;
    private final ShowtimeSnapshotPort showtimeSnapshotPort;
    private final ShowtimeSchedulePolicy policy;
    private final Clock clock;

    @Override
    public Showtime update(UUID showtimeId, UpdateShowtimeCommand command) {
        if (command == null || (command.getStartTime() == null && command.getBasePrice() == null)) {
            throw new InvalidShowtimeException("Không có gì để cập nhật: hãy gửi giờ chiếu và/hoặc giá vé");
        }
        Instant now = clock.instant();
        Showtime showtime = lock(showtimeId);
        showtime.assertEditable(now);
        refuseIfSeatsTaken(showtime, now, "sửa");

        if (command.getStartTime() != null && !command.getStartTime().equals(showtime.getStartTime())) {
            showtime.reschedule(command.getStartTime(), now);
            showtimeSnapshotPort.lockScreen(showtime.getScreenId());
            ScheduleRules.refuseOverlap(showtimeRepositoryPort, policy, showtime.getScreenId(),
                    showtime.getStartTime(), showtime.getEndTime(), showtime.getId());
        }

        if (command.getBasePrice() != null && command.getBasePrice().compareTo(showtime.getBasePrice()) != 0) {
            BigDecimal oldBase = showtime.getBasePrice();
            showtime.changeBasePrice(command.getBasePrice(), now);
            List<ShowtimeSeat> seats = showtimeSeatRepositoryPort.findByShowtimeId(showtimeId);
            seats.forEach(seat -> seat.setPrice(Showtime.repriceSeat(seat.getPrice(), oldBase, command.getBasePrice())));
            showtimeSeatRepositoryPort.saveAll(seats);
        }

        return showtimeRepositoryPort.save(showtime);
    }

    @Override
    public Showtime cancel(UUID showtimeId) {
        Instant now = clock.instant();
        Showtime showtime = lock(showtimeId);
        if (showtime.isCancelled()) {
            return showtime; // idempotent
        }
        refuseIfSeatsTaken(showtime, now, "huỷ");
        showtime.cancel();

        List<ShowtimeSeat> seats = showtimeSeatRepositoryPort.findByShowtimeId(showtimeId);
        seats.forEach(ShowtimeSeat::block);
        showtimeSeatRepositoryPort.saveAll(seats);
        return showtimeRepositoryPort.save(showtime);
    }

    private Showtime lock(UUID showtimeId) {
        return showtimeRepositoryPort.findByIdWithLock(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId));
    }

    private void refuseIfSeatsTaken(Showtime showtime, Instant now, String action) {
        long taken = showtimeSeatRepositoryPort.countTakenSeats(showtime.getId(), now);
        if (taken > 0) {
            throw new ShowtimeConflictException("Suất chiếu " + showtime.getId() + " có " + taken
                    + " ghế đang được giữ hoặc đã bán nên không thể " + action);
        }
    }
}
