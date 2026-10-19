package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.dto.PageResult;
import com.mv.showtimeservice.application.port.in.GetShowtimeUseCase;
import com.mv.showtimeservice.application.port.out.ShowtimeRepositoryPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSeatRepositoryPort;
import com.mv.showtimeservice.application.query.SearchShowtimesQuery;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeStatus;
import com.mv.showtimeservice.domain.model.showtime.exception.ShowtimeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetShowtimeService implements GetShowtimeUseCase {

    private static final Set<ShowtimeStatus> ON_SALE = EnumSet.of(ShowtimeStatus.SCHEDULED, ShowtimeStatus.SELLING);

    private final ShowtimeRepositoryPort showtimeRepositoryPort;
    private final ShowtimeSeatRepositoryPort showtimeSeatRepositoryPort;
    private final Clock clock;

    @Override
    public Showtime getById(UUID showtimeId) {
        return showtimeRepositoryPort.findById(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId));
    }

    @Override
    public PageResult<Showtime> search(SearchShowtimesQuery q) {
        int page = Math.max(0, q.getPage());
        int size = Math.min(100, Math.max(1, q.getSize()));
        Set<ShowtimeStatus> statuses = q.getStatuses() == null || q.getStatuses().isEmpty() ? ON_SALE : q.getStatuses();
        Instant from = q.getFrom() != null ? q.getFrom() : clock.instant();
        return showtimeRepositoryPort.search(q.getMovieId(), q.getCinemaId(), q.getScreenId(), from, q.getTo(),
                statuses, page, size);
    }

    @Override
    public List<ShowtimeSeat> getSeats(UUID showtimeId) {
        getById(showtimeId);
        return showtimeSeatRepositoryPort.findByShowtimeId(showtimeId).stream()
                .sorted(Comparator.comparing(ShowtimeSeat::getSeatLabel, SeatLabelOrder.INSTANCE))
                .toList();
    }

    /** "A2" before "A10": row letters, then seat number. */
    enum SeatLabelOrder implements Comparator<String> {
        INSTANCE;

        @Override
        public int compare(String a, String b) {
            String rowA = a.replaceAll("[0-9]", "");
            String rowB = b.replaceAll("[0-9]", "");
            int byRow = rowA.compareTo(rowB);
            if (byRow != 0) {
                return byRow;
            }
            return Integer.compare(number(a), number(b));
        }

        private static int number(String label) {
            String digits = label.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        }
    }
}
