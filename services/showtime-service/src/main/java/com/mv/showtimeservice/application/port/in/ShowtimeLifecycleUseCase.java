package com.mv.showtimeservice.application.port.in;

public interface ShowtimeLifecycleUseCase {
    /** Moves showtimes along SCHEDULED -> SELLING -> ENDED. @return number of showtimes changed */
    int advance();
}
