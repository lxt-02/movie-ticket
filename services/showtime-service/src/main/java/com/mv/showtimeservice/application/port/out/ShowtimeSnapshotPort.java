package com.mv.showtimeservice.application.port.out;

public interface ShowtimeSnapshotPort {
    void saveMovieSnapshot(MovieClientPort.MovieSnapshot movie);

    void saveScreenSnapshot(CinemaClientPort.CinemaSnapshot cinema, CinemaClientPort.ScreenLayoutSnapshot screen);
}
