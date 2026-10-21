package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.command.CreateShowtimeCommand;
import com.mv.showtimeservice.application.port.in.CreateShowtimeUseCase;
import com.mv.showtimeservice.application.port.out.CinemaClientPort;
import com.mv.showtimeservice.application.port.out.MovieClientPort;
import com.mv.showtimeservice.application.port.out.ShowtimeRepositoryPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSeatRepositoryPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSnapshotPort;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeSeatStatus;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeStatus;
import lombok.RequiredArgsConstructor;
import com.mv.showtimeservice.application.dto.ShowtimeSchedulePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateShowtimeService implements CreateShowtimeUseCase {

    private static final BigDecimal DEFAULT_PRICE_MULTIPLIER = BigDecimal.ONE;

    private final MovieClientPort movieClientPort;
    private final CinemaClientPort cinemaClientPort;
    private final ShowtimeSnapshotPort showtimeSnapshotPort;
    private final ShowtimeRepositoryPort showtimeRepositoryPort;
    private final ShowtimeSeatRepositoryPort showtimeSeatRepositoryPort;
    private final ShowtimeSchedulePolicy schedulePolicy;

    @Override
    @Transactional
    public Showtime execute(CreateShowtimeCommand command) {
        validateCommand(command);

        MovieClientPort.MovieSnapshot movie = movieClientPort.getMovie(command.getMovieId());
        CinemaClientPort.CinemaSnapshot cinema = cinemaClientPort.getCinema(command.getCinemaId());
        CinemaClientPort.ScreenLayoutSnapshot screen = cinemaClientPort.getScreenLayout(command.getScreenId());

        validateSourceState(command, movie, cinema, screen);

        UUID showtimeId = UUID.randomUUID();
        Instant endTime = command.getStartTime().plusSeconds((long) movie.durationMinutes() * 60);

        showtimeSnapshotPort.saveMovieSnapshot(movie);
        showtimeSnapshotPort.saveScreenSnapshot(cinema, screen);

        // One showtime at a time per screen: lock the screen, then check the schedule.
        showtimeSnapshotPort.lockScreen(screen.screenId());
        ScheduleRules.refuseOverlap(showtimeRepositoryPort, schedulePolicy, screen.screenId(),
                command.getStartTime(), endTime, null);

        Showtime showtime = Showtime.builder()
                .id(showtimeId)
                .movieId(movie.movieId())
                .screenId(screen.screenId())
                .movieTitle(movie.title())
                .cinemaId(cinema.cinemaId())
                .cinemaName(cinema.name())
                .screenName(screen.screenName())
                .startTime(command.getStartTime())
                .endTime(endTime)
                .basePrice(command.getBasePrice())
                .status(ShowtimeStatus.SCHEDULED)
                .createdAt(Instant.now())
                .build();

        Showtime savedShowtime = showtimeRepositoryPort.save(showtime);
        List<ShowtimeSeat> seats = buildShowtimeSeats(savedShowtime.getId(), command.getBasePrice(), screen.seats());
        showtimeSeatRepositoryPort.saveAll(seats);

        return savedShowtime;
    }

    private void validateCommand(CreateShowtimeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu suất chiếu");
        }
        if (command.getMovieId() == null) {
            throw new IllegalArgumentException("Phim là bắt buộc");
        }
        if (command.getCinemaId() == null) {
            throw new IllegalArgumentException("Rạp là bắt buộc");
        }
        if (command.getScreenId() == null) {
            throw new IllegalArgumentException("Phòng chiếu là bắt buộc");
        }
        if (command.getStartTime() == null || !command.getStartTime().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Giờ chiếu phải ở tương lai");
        }
        if (command.getBasePrice() == null || command.getBasePrice().signum() < 0) {
            throw new IllegalArgumentException("Giá vé không được âm");
        }
    }

    private void validateSourceState(
            CreateShowtimeCommand command,
            MovieClientPort.MovieSnapshot movie,
            CinemaClientPort.CinemaSnapshot cinema,
            CinemaClientPort.ScreenLayoutSnapshot screen
    ) {
        if (!"NOW_SHOWING".equalsIgnoreCase(movie.status())) {
            throw new IllegalArgumentException("Phim không ở trạng thái được xếp lịch chiếu");
        }
        if (!"ACTIVE".equalsIgnoreCase(cinema.status())) {
            throw new IllegalArgumentException("Rạp không hoạt động");
        }
        if (!command.getCinemaId().equals(screen.cinemaId())) {
            throw new IllegalArgumentException("Phòng chiếu không thuộc rạp đã chọn");
        }
        if (!"ACTIVE".equalsIgnoreCase(screen.status())) {
            throw new IllegalArgumentException("Phòng chiếu không hoạt động");
        }
        if (screen.seats() == null || screen.seats().isEmpty()) {
            throw new IllegalArgumentException("Phòng chiếu chưa có sơ đồ ghế");
        }
    }

    private List<ShowtimeSeat> buildShowtimeSeats(
            UUID showtimeId,
            BigDecimal basePrice,
            List<CinemaClientPort.SeatSnapshot> seats
    ) {
        return seats.stream()
                .map(seat -> ShowtimeSeat.builder()
                        .id(UUID.randomUUID())
                        .showtimeId(showtimeId)
                        .seatId(seat.seatId())
                        .seatLabel(seat.label())
                        .seatType(seat.seatType())
                        .price(basePrice.multiply(seat.priceMultiplier() != null ? seat.priceMultiplier() : DEFAULT_PRICE_MULTIPLIER))
                        .status("ACTIVE".equalsIgnoreCase(seat.status()) ? ShowtimeSeatStatus.AVAILABLE : ShowtimeSeatStatus.BLOCKED)
                        .build())
                .toList();
    }
}
