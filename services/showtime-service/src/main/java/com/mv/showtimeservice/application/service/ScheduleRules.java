package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.dto.ShowtimeSchedulePolicy;
import com.mv.showtimeservice.application.port.out.ShowtimeRepositoryPort;
import com.mv.showtimeservice.domain.model.showtime.exception.ShowtimeConflictException;

import java.time.Instant;
import java.util.UUID;

/** Shared by create and update: a screen shows one movie at a time, with a cleaning gap in between. */
final class ScheduleRules {

    private ScheduleRules() {
    }

    static void refuseOverlap(ShowtimeRepositoryPort repository, ShowtimeSchedulePolicy policy, UUID screenId,
                              Instant start, Instant end, UUID excludeShowtimeId) {
        Instant from = start.minus(policy.cleaningBuffer());
        Instant to = end.plus(policy.cleaningBuffer());
        if (repository.existsOverlapping(screenId, from, to, excludeShowtimeId)) {
            throw new ShowtimeConflictException("Phòng " + screenId + " đã có suất chiếu trong khoảng "
                    + from + " đến " + to + " (tính cả " + policy.cleaningBuffer().toMinutes()
                    + " phút dọn phòng)");
        }
    }
}
