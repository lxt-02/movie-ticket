package com.mv.showtimeservice.application.port.out;

public interface ShowtimeSnapshotPort {
    void saveMovieSnapshot(MovieClientPort.MovieSnapshot movie);

    void saveScreenSnapshot(CinemaClientPort.CinemaSnapshot cinema, CinemaClientPort.ScreenLayoutSnapshot screen);

    /**
     * Locks the screen snapshot row (SELECT ... FOR UPDATE) until the transaction ends, so two admins cannot
     * schedule overlapping showtimes on the same screen at the same moment. No-op if the snapshot does not exist.
     */
    void lockScreen(java.util.UUID screenId);
}
