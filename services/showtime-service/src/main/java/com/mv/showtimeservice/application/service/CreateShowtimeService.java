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
            throw new IllegalArgumentException("Create showtime command must not be null");
        }
        if (command.getMovieId() == null) {
            throw new IllegalArgumentException("movieId must not be null");
        }
        if (command.getCinemaId() == null) {
            throw new IllegalArgumentException("cinemaId must not be null");
        }
        if (command.getScreenId() == null) {
            throw new IllegalArgumentException("screenId must not be null");
        }
        if (command.getStartTime() == null || !command.getStartTime().isAfter(Instant.now())) {
            throw new IllegalArgumentException("startTime must be in the future");
        }
        if (command.getBasePrice() == null || command.getBasePrice().signum() < 0) {
            throw new IllegalArgumentException("basePrice must be greater than or equal to 0");
        }
    }

    private void validateSourceState(
            CreateShowtimeCommand command,
            MovieClientPort.MovieSnapshot movie,
            CinemaClientPort.CinemaSnapshot cinema,
            CinemaClientPort.ScreenLayoutSnapshot screen
    ) {
        if (!"NOW_SHOWING".equalsIgnoreCase(movie.status())) {
            throw new IllegalArgumentException("Movie is not available for scheduling");
        }
        if (!"ACTIVE".equalsIgnoreCase(cinema.status())) {
            throw new IllegalArgumentException("Cinema is not active");
        }
        if (!command.getCinemaId().equals(screen.cinemaId())) {
            throw new IllegalArgumentException("Screen does not belong to requested cinema");
        }
        if (!"ACTIVE".equalsIgnoreCase(screen.status())) {
            throw new IllegalArgumentException("Screen is not active");
        }
        if (screen.seats() == null || screen.seats().isEmpty()) {
            throw new IllegalArgumentException("Screen layout has no seats");
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
