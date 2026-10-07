package com.mv.showtimeservice.infrastructure.adapter;

import com.mv.showtimeservice.application.port.out.CinemaClientPort;
import com.mv.showtimeservice.application.port.out.MovieClientPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSnapshotPort;
import com.mv.showtimeservice.infrastructure.persistence.entity.MovieSnapshotEntity;
import com.mv.showtimeservice.infrastructure.persistence.entity.ScreenSnapshotEntity;
import com.mv.showtimeservice.infrastructure.persistence.repository.MovieSnapshotJpaRepository;
import com.mv.showtimeservice.infrastructure.persistence.repository.ScreenSnapshotJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ShowtimeSnapshotAdapter implements ShowtimeSnapshotPort {

    private final MovieSnapshotJpaRepository movieSnapshotJpaRepository;
    private final ScreenSnapshotJpaRepository screenSnapshotJpaRepository;

    @Override
    public void saveMovieSnapshot(MovieClientPort.MovieSnapshot movie) {
        movieSnapshotJpaRepository.save(MovieSnapshotEntity.builder()
                .movieId(movie.movieId())
                .title(movie.title())
                .durationMinutes(movie.durationMinutes())
                .ageRating(movie.ageRating())
                .status(movie.status())
                .version(1)
                .updatedAt(Instant.now())
                .build());
    }

    @Override
    public void saveScreenSnapshot(CinemaClientPort.CinemaSnapshot cinema, CinemaClientPort.ScreenLayoutSnapshot screen) {
        screenSnapshotJpaRepository.save(ScreenSnapshotEntity.builder()
                .screenId(screen.screenId())
                .cinemaId(cinema.cinemaId())
                .cinemaName(cinema.name())
                .screenName(screen.screenName())
                .screenType(screen.screenType())
                .totalSeats(screen.totalSeats())
                .status(screen.status())
                .version(1)
                .updatedAt(Instant.now())
                .build());
    }
}
