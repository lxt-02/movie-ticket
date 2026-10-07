package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.command.CreateShowtimeCommand;
import com.mv.showtimeservice.application.port.out.CinemaClientPort;
import com.mv.showtimeservice.application.port.out.MovieClientPort;
import com.mv.showtimeservice.application.port.out.ShowtimeRepositoryPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSeatRepositoryPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSnapshotPort;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeSeatStatus;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateShowtimeServiceTest {

    @Mock
    private MovieClientPort movieClientPort;

    @Mock
    private CinemaClientPort cinemaClientPort;

    @Mock
    private ShowtimeSnapshotPort showtimeSnapshotPort;

    @Mock
    private ShowtimeRepositoryPort showtimeRepositoryPort;

    @Mock
    private ShowtimeSeatRepositoryPort showtimeSeatRepositoryPort;

    private CreateShowtimeService createShowtimeService;

    @BeforeEach
    void setUp() {
        createShowtimeService = new CreateShowtimeService(
                movieClientPort,
                cinemaClientPort,
                showtimeSnapshotPort,
                showtimeRepositoryPort,
                showtimeSeatRepositoryPort
        );
    }

    @Test
    void shouldCreateShowtimeFromMovieCinemaAndScreenLayout() {
        UUID movieId = UUID.randomUUID();
        UUID cinemaId = UUID.randomUUID();
        UUID screenId = UUID.randomUUID();
        UUID activeSeatId = UUID.randomUUID();
        UUID inactiveSeatId = UUID.randomUUID();
        Instant startTime = Instant.now().plusSeconds(3600);

        MovieClientPort.MovieSnapshot movie = new MovieClientPort.MovieSnapshot(movieId, "Movie 1", 120, "T13", "NOW_SHOWING");
        CinemaClientPort.CinemaSnapshot cinema = new CinemaClientPort.CinemaSnapshot(cinemaId, "Cinema 1", "ACTIVE");
        CinemaClientPort.ScreenLayoutSnapshot screen = new CinemaClientPort.ScreenLayoutSnapshot(
                screenId,
                cinemaId,
                "Screen 1",
                "2D",
                2,
                "ACTIVE",
                List.of(
                        new CinemaClientPort.SeatSnapshot(activeSeatId, "STANDARD", new BigDecimal("1.00"), "A1", "ACTIVE"),
                        new CinemaClientPort.SeatSnapshot(inactiveSeatId, "VIP", new BigDecimal("1.50"), "A2", "INACTIVE")
                )
        );
        CreateShowtimeCommand command = CreateShowtimeCommand.builder()
                .movieId(movieId)
                .cinemaId(cinemaId)
                .screenId(screenId)
                .startTime(startTime)
                .basePrice(new BigDecimal("100000.00"))
                .build();

        when(movieClientPort.getMovie(movieId)).thenReturn(movie);
        when(cinemaClientPort.getCinema(cinemaId)).thenReturn(cinema);
        when(cinemaClientPort.getScreenLayout(screenId)).thenReturn(screen);
        when(showtimeRepositoryPort.save(any(Showtime.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Showtime result = createShowtimeService.execute(command);

        assertEquals(movieId, result.getMovieId());
        assertEquals(screenId, result.getScreenId());
        assertEquals(cinemaId, result.getCinemaId());
        assertEquals(startTime.plusSeconds(7200), result.getEndTime());
        assertEquals(ShowtimeStatus.SCHEDULED, result.getStatus());
        verify(showtimeSnapshotPort).saveMovieSnapshot(movie);
        verify(showtimeSnapshotPort).saveScreenSnapshot(cinema, screen);

        ArgumentCaptor<List<ShowtimeSeat>> seatsCaptor = ArgumentCaptor.captor();
        verify(showtimeSeatRepositoryPort).saveAll(seatsCaptor.capture());
        List<ShowtimeSeat> seats = seatsCaptor.getValue();
        assertEquals(2, seats.size());
        assertTrue(seats.stream().anyMatch(seat -> seat.getSeatId().equals(activeSeatId)
                && seat.getStatus() == ShowtimeSeatStatus.AVAILABLE
                && seat.getPrice().compareTo(new BigDecimal("100000.0000")) == 0));
        assertTrue(seats.stream().anyMatch(seat -> seat.getSeatId().equals(inactiveSeatId)
                && seat.getStatus() == ShowtimeSeatStatus.BLOCKED
                && seat.getPrice().compareTo(new BigDecimal("150000.0000")) == 0));
    }

    @Test
    void shouldRejectMovieThatIsNotNowShowing() {
        UUID movieId = UUID.randomUUID();
        UUID cinemaId = UUID.randomUUID();
        UUID screenId = UUID.randomUUID();
        CreateShowtimeCommand command = CreateShowtimeCommand.builder()
                .movieId(movieId)
                .cinemaId(cinemaId)
                .screenId(screenId)
                .startTime(Instant.now().plusSeconds(3600))
                .basePrice(BigDecimal.ONE)
                .build();

        when(movieClientPort.getMovie(movieId)).thenReturn(new MovieClientPort.MovieSnapshot(movieId, "Movie 1", 120, "T13", "COMING_SOON"));
        when(cinemaClientPort.getCinema(cinemaId)).thenReturn(new CinemaClientPort.CinemaSnapshot(cinemaId, "Cinema 1", "ACTIVE"));
        when(cinemaClientPort.getScreenLayout(screenId)).thenReturn(new CinemaClientPort.ScreenLayoutSnapshot(
                screenId,
                cinemaId,
                "Screen 1",
                "2D",
                1,
                "ACTIVE",
                List.of(new CinemaClientPort.SeatSnapshot(UUID.randomUUID(), "STANDARD", BigDecimal.ONE, "A1", "ACTIVE"))
        ));

        assertThrows(IllegalArgumentException.class, () -> createShowtimeService.execute(command));

        verify(showtimeRepositoryPort, never()).save(any());
        verify(showtimeSeatRepositoryPort, never()).saveAll(any());
    }

    @Test
    void shouldRejectScreenThatDoesNotBelongToCinema() {
        UUID movieId = UUID.randomUUID();
        UUID cinemaId = UUID.randomUUID();
        UUID screenId = UUID.randomUUID();
        CreateShowtimeCommand command = CreateShowtimeCommand.builder()
                .movieId(movieId)
                .cinemaId(cinemaId)
                .screenId(screenId)
                .startTime(Instant.now().plusSeconds(3600))
                .basePrice(BigDecimal.ONE)
                .build();

        when(movieClientPort.getMovie(movieId)).thenReturn(new MovieClientPort.MovieSnapshot(movieId, "Movie 1", 120, "T13", "NOW_SHOWING"));
        when(cinemaClientPort.getCinema(cinemaId)).thenReturn(new CinemaClientPort.CinemaSnapshot(cinemaId, "Cinema 1", "ACTIVE"));
        when(cinemaClientPort.getScreenLayout(screenId)).thenReturn(new CinemaClientPort.ScreenLayoutSnapshot(
                screenId,
                UUID.randomUUID(),
                "Screen 1",
                "2D",
                1,
                "ACTIVE",
                List.of(new CinemaClientPort.SeatSnapshot(UUID.randomUUID(), "STANDARD", BigDecimal.ONE, "A1", "ACTIVE"))
        ));

        assertThrows(IllegalArgumentException.class, () -> createShowtimeService.execute(command));

        verify(showtimeRepositoryPort, never()).save(any());
        verify(showtimeSeatRepositoryPort, never()).saveAll(any());
    }
}
